#!/bin/bash
set -uo pipefail

# ─────────────────────────────────────────────────────────────
# Ounce 무중단(Blue/Green) 배포 스크립트 — 운영 서버(EC2)용
#
# 트래픽 게이트는 "호스트에 apt 로 설치된 nginx" 다.
#   - 설정:   /etc/nginx/sites-available/default
#   - include: /etc/nginx/conf.d/service-env.inc  ← 여기서 $service_url 결정
#   - 프록시 대상: http://127.0.0.1:8080(blue) 또는 8081(green)
#
# docker-compose 의 nginx 컨테이너는 80/443 을 열지 않아 트래픽 경로 밖이다.
# 예전 스크립트가 그 컨테이너를 고치고 reload 하는 바람에, 색 전환이
# 실제 트래픽에 아무 영향이 없었다. 이 스크립트는 호스트 nginx 만 다룬다.
# ─────────────────────────────────────────────────────────────

PROJECT_DIR="/home/ubuntu/ounce"
ENV_FILE="/etc/nginx/conf.d/service-env.inc"   # 호스트 nginx 가 실제로 읽는 파일
COMPOSE="docker compose -f docker-compose.yml"

HEALTH_PATH="/"        # 앱이 2xx/3xx 를 주는 경로 (actuator 를 붙였다면 /actuator/health 권장)
HEALTH_RETRY=30        # 30회 x 3초 = 최대 90초 (+ curl 타임아웃)
HEALTH_INTERVAL=3
CURL_TIMEOUT=15        # 콜드 스타트 첫 요청은 5초를 넘긴다
DRAIN_SECONDS=25       # nginx 워커가 옛 설정으로 보내는 요청을 흘려보내는 시간
STOP_TIMEOUT=30        # 구버전에 주는 graceful shutdown 시간

cd "$PROJECT_DIR" || exit 1

echo "🚀 Ounce 배포 스크립트를 시작합니다."

# ── 0. sudo 권한 선확인 ─────────────────────────────────────
# CI 는 비대화형이라, sudo 가 비밀번호를 물으면 여기서 조용히 멈춘다.
# 트래픽을 전환할 수 없는 상태라면 컨테이너를 띄우기 전에 멈추는 게 낫다.
if ! sudo -n true 2>/dev/null; then
    echo "❌ sudo 가 비밀번호를 요구합니다. /etc/sudoers.d/ 설정을 확인하세요."
    exit 1
fi

# ── 1. 현재 서비스 중인 색 판별 ──────────────────────────────
# nginx 가 지금 이 순간 어디로 프록시하고 있는지가 유일한 진실이다.
if [ ! -s "$ENV_FILE" ]; then
    echo "❌ $ENV_FILE 이 없거나 비어 있습니다."
    echo "   복구: echo 'set \$service_url http://127.0.0.1:8081;' | sudo tee $ENV_FILE"
    exit 1
fi

if grep -q "8080" "$ENV_FILE"; then
    OLD_COLOR="blue";  OLD_PORT=8080; TARGET_COLOR="green"; TARGET_PORT=8081
elif grep -q "8081" "$ENV_FILE"; then
    OLD_COLOR="green"; OLD_PORT=8081; TARGET_COLOR="blue";  TARGET_PORT=8080
else
    echo "❌ $ENV_FILE 에서 현재 색(8080/8081)을 읽지 못했습니다. 내용:"
    cat "$ENV_FILE"
    exit 1
fi

echo "   현재 서비스 중: $OLD_COLOR($OLD_PORT)  →  배포 대상: $TARGET_COLOR($TARGET_PORT)"

# ── 2. 신버전 컨테이너 기동 ─────────────────────────────────
echo "🐳 $TARGET_COLOR 컨테이너를 최신 이미지로 실행합니다."
$COMPOSE pull "app-$TARGET_COLOR" || { echo "❌ 이미지 pull 실패"; exit 1; }
# --no-deps       : DB/Redis 등 다른 서비스를 건드리지 않는다.
# --force-recreate: 태그가 latest 라 컨테이너가 남아있어도 확실히 새로 만든다.
$COMPOSE up -d --no-deps --force-recreate "app-$TARGET_COLOR" \
    || { echo "❌ $TARGET_COLOR 기동 실패"; exit 1; }

# ── 3. 헬스 체크 ────────────────────────────────────────────
# 문자열이 아니라 HTTP 상태 코드로 판정한다.
# 컨테이너가 죽어 있으면 90초를 기다릴 이유가 없으므로 즉시 빠진다.
echo "⏳ $TARGET_COLOR 헬스 체크 (http://localhost:$TARGET_PORT$HEALTH_PATH)"
HEALTHY=0
for (( i=1; i<=HEALTH_RETRY; i++ )); do
    STATE=$(docker inspect -f '{{.State.Status}}' "app-$TARGET_COLOR" 2>/dev/null)
    if [ "$STATE" != "running" ]; then
        echo "❌ app-$TARGET_COLOR 가 실행 중이 아닙니다 (상태: ${STATE:-없음}). 즉시 중단합니다."
        break
    fi

    CODE=$(curl -s -o /dev/null -m "$CURL_TIMEOUT" -w '%{http_code}' \
             "http://localhost:$TARGET_PORT$HEALTH_PATH" 2>/dev/null)
    # 2xx ~ 3xx 면 앱이 정상 응답한 것으로 본다 (Security 리다이렉트 포함)
    if [[ "$CODE" =~ ^[23] ]]; then
        echo "✅ 헬스 체크 성공 (HTTP $CODE, ${i}번째 시도)"
        HEALTHY=1
        break
    fi
    echo "   서버가 켜지는 중입니다... (HTTP ${CODE:-000}) ($i/$HEALTH_RETRY)"
    sleep "$HEALTH_INTERVAL"
done

if [ "$HEALTHY" -ne 1 ]; then
    echo "❌ 헬스 체크 실패. 트래픽은 그대로 $OLD_COLOR 에 둡니다 (서비스 영향 없음)."
    echo "   ── $TARGET_COLOR 컨테이너 로그 마지막 80줄 ──"
    $COMPOSE logs --tail=80 "app-$TARGET_COLOR"
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    $COMPOSE rm -f "app-$TARGET_COLOR"
    exit 1
fi

# ── 4. 워밍업 ───────────────────────────────────────────────
# 첫 요청은 JIT / 커넥션풀 / Thymeleaf 템플릿 컴파일 때문에 느리다.
# 트래픽을 넘기기 전에 우리가 대신 몇 번 맞아준다.
echo "🔥 워밍업 요청 3회"
for _ in 1 2 3; do
    curl -s -o /dev/null -m 20 "http://localhost:$TARGET_PORT$HEALTH_PATH"
done

# ── 5. 호스트 nginx 트래픽 전환 ─────────────────────────────
echo "🔄 Nginx 트래픽을 $TARGET_COLOR 로 전환합니다."
sudo cp "$ENV_FILE" "$ENV_FILE.bak"
echo "set \$service_url http://127.0.0.1:$TARGET_PORT;" | sudo tee "$ENV_FILE" > /dev/null

if ! sudo nginx -t; then
    echo "❌ nginx 설정 검증 실패. 원래 설정으로 롤백합니다."
    sudo mv "$ENV_FILE.bak" "$ENV_FILE"
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi

if ! sudo systemctl reload nginx; then
    echo "❌ nginx reload 실패. 원래 설정으로 롤백합니다."
    sudo mv "$ENV_FILE.bak" "$ENV_FILE"
    sudo systemctl reload nginx || true
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi

# 전환이 실제로 먹었는지 도메인이 아니라 로컬에서 확인한다.
SWITCHED_CODE=$(curl -s -o /dev/null -m 10 -w '%{http_code}' -H "Host: ouncefresh.com" \
                  "http://127.0.0.1$HEALTH_PATH" 2>/dev/null)
if [[ ! "$SWITCHED_CODE" =~ ^[23] ]]; then
    echo "❌ 전환 후 nginx 응답이 비정상입니다 (HTTP ${SWITCHED_CODE:-000}). 롤백합니다."
    sudo mv "$ENV_FILE.bak" "$ENV_FILE"
    sudo systemctl reload nginx || true
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi

sudo rm -f "$ENV_FILE.bak"
echo "✅ Nginx 트래픽 전환 완료! (HTTP $SWITCHED_CODE)"

# ── 6. 드레인 ───────────────────────────────────────────────
# reload 는 즉시 끝나지만, 기존 워커 프로세스는 아직 살아서
# 이미 맺어진 keep-alive 커넥션의 요청을 "옛날 설정"으로 보낸다.
# 여기서 안 기다리고 구버전을 죽이면 그 요청들이 전부 502 다.
echo "⏳ 기존 커넥션 드레인 대기 (${DRAIN_SECONDS}초)..."
sleep "$DRAIN_SECONDS"

# ── 7. 구버전 graceful 종료 ─────────────────────────────────
echo "🛑 기존 $OLD_COLOR 컨테이너를 종료합니다 (최대 ${STOP_TIMEOUT}초 대기)."
$COMPOSE stop -t "$STOP_TIMEOUT" "app-$OLD_COLOR"
$COMPOSE rm -f "app-$OLD_COLOR"

# ── 8. 디스크 정리 ──────────────────────────────────────────
# latest 태그를 계속 덮어쓰면 예전 이미지가 태그만 잃고 디스크에 쌓인다.
docker image prune -f > /dev/null 2>&1 || true

echo "🎉 배포 완료. 현재 서비스 중: $TARGET_COLOR"