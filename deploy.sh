#!/bin/bash
set -uo pipefail

# ─────────────────────────────────────────────────────────────
# Ounce 무중단(Blue/Green) 배포 스크립트  —  운영 서버(EC2)용
#
# 이전 버전이 502 를 냈던 이유와 그 대응:
#   1. `docker ps | grep ounce-app-blue-1` 로 현재 색을 판별했는데,
#      실제 컨테이너 이름은 app-blue 라서 절대 매칭되지 않았다.
#      → 매번 TARGET=blue 가 되어 "지금 트래픽 받는 컨테이너를 재생성"했다.
#      → 이제 nginx 가 실제로 읽는 service-env.inc 에서 판별한다.
#   2. nginx -s reload 성공 여부를 확인하지 않았다. → nginx -t 선검증.
#   3. reload 직후 1초 만에 구버전을 죽였다. 옛 워커가 아직 구버전으로
#      요청을 보내는 중이라 그게 전부 502 였다. → DRAIN_SECONDS 대기.
#   4. 종료가 즉시 kill 이었다. → graceful shutdown + stop -t.
# ─────────────────────────────────────────────────────────────

PROJECT_DIR="/home/ubuntu/ounce"
ENV_FILE="$PROJECT_DIR/nginx/conf.d/service-env.inc"
COMPOSE="docker compose -f docker-compose.yml"

HEALTH_PATH="/"       # 앱이 200 또는 3xx 를 주는 경로
HEALTH_RETRY=80       
HEALTH_INTERVAL=3
DRAIN_SECONDS=25      # nginx worker_shutdown_timeout(20s) 보다 커야 함
STOP_TIMEOUT=30       # 구버전에 주는 graceful shutdown 시간

cd "$PROJECT_DIR" || exit 1

echo "🚀 Ounce 배포 스크립트를 시작합니다."

# ── 0. 색 상태 파일 복구 ────────────────────────────────────
# service-env.inc 는 설정이 아니라 "지금 어느 색이 서비스 중인지"라는 런타임 상태다.
# 그래서 CI 의 scp 대상에서 제외했다(배포마다 덮으면 색 판별이 실제와 어긋난다).
# 대신 파일이 유실됐거나 서버를 새로 세팅한 경우를 여기서 복구한다.
# 판단 기준은 "지금 실제로 떠 있는 컨테이너"다.
if [ ! -s "$ENV_FILE" ]; then
    mkdir -p "$(dirname "$ENV_FILE")"
    if docker ps --format '{{.Names}}' | grep -qx "app-green"; then
        SEED_COLOR="app-green"
    else
        SEED_COLOR="app-blue"
    fi
    echo "⚠️  $ENV_FILE 이 없습니다. 실행 중인 컨테이너 기준으로 새로 만듭니다: $SEED_COLOR"
    echo "set \$service_url $SEED_COLOR:8080;" > "$ENV_FILE"
fi

# ── 1. 현재 서비스 중인 색 판별 ──────────────────────────────
# docker ps 이름 규칙에 의존하지 않는다.
# nginx 가 지금 이 순간 어디로 프록시하고 있는지가 유일한 진실이다.
if grep -q "app-blue" "$ENV_FILE" 2>/dev/null; then
    OLD_COLOR="blue";  TARGET_COLOR="green"; TARGET_PORT=8081
elif grep -q "app-green" "$ENV_FILE" 2>/dev/null; then
    OLD_COLOR="green"; TARGET_COLOR="blue";  TARGET_PORT=8080
else
    echo "❌ $ENV_FILE 에서 현재 색을 읽지 못했습니다. 파일 내용을 확인하세요."
    cat "$ENV_FILE" 2>/dev/null
    exit 1
fi

echo "   현재 서비스 중: $OLD_COLOR  →  배포 대상: $TARGET_COLOR (호스트 포트 $TARGET_PORT)"

# ── 2. 신버전 컨테이너 기동 ─────────────────────────────────
echo "🐳 $TARGET_COLOR 컨테이너를 최신 이미지로 실행합니다."
$COMPOSE pull "app-$TARGET_COLOR" || { echo "❌ 이미지 pull 실패"; exit 1; }
# --no-deps      : DB/Redis/nginx 등 다른 서비스를 건드리지 않는다.
# --force-recreate: 태그가 latest 라 컨테이너가 남아있어도 확실히 새로 만든다.
$COMPOSE up -d --no-deps --force-recreate "app-$TARGET_COLOR" \
    || { echo "❌ $TARGET_COLOR 기동 실패"; exit 1; }

# ── 3. 헬스 체크 ────────────────────────────────────────────
# 문자열 매칭이 아니라 HTTP 상태 코드로 판정한다.
# (에러 페이지에 "Ounce" 가 들어있으면 예전 방식은 통과해버렸다)
echo "⏳ $TARGET_COLOR 헬스 체크 (http://localhost:$TARGET_PORT$HEALTH_PATH)"
HEALTHY=0
for (( i=1; i<=HEALTH_RETRY; i++ )); do
    CODE=$(curl -s -o /dev/null -m 5 -w '%{http_code}' \
             "http://localhost:$TARGET_PORT$HEALTH_PATH" 2>/dev/null)
    # 200 ~ 399 면 앱이 정상 응답한 것으로 본다 (Security 리다이렉트 포함)
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
    echo "   ── $TARGET_COLOR 컨테이너 로그 마지막 50줄 ──"
    $COMPOSE logs --tail=50 "app-$TARGET_COLOR"
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    $COMPOSE rm -f "app-$TARGET_COLOR"
    exit 1
fi

# ── 3-b. 도커 네트워크 경로 검증 ────────────────────────────
# 위 헬스체크는 호스트 포트를 봤다. nginx 는 도커 DNS 로 app-green:8080 을
# 찾아가므로, 그 경로가 실제로 뚫리는지 한 번 더 확인한다.
# (nginx:alpine 에 wget 이 없을 수도 있으니 실패해도 경고만 남긴다)
if $COMPOSE exec -T nginx wget -q -T 5 -O /dev/null \
       "http://app-$TARGET_COLOR:8080$HEALTH_PATH" 2>/dev/null; then
    echo "✅ 도커 네트워크 경로(app-$TARGET_COLOR:8080) 확인 완료"
else
    echo "⚠️  nginx 컨테이너에서의 확인은 건너뜁니다 (wget 부재 또는 응답 실패)"
fi

# ── 4. 워밍업 ───────────────────────────────────────────────
# 첫 요청은 JIT / 커넥션풀 / Thymeleaf 템플릿 컴파일 때문에 느립니다.
# 트래픽을 넘기기 전에 우리가 대신 몇 번 맞아줍니다.
echo "🔥 워밍업 요청 3회"
for _ in 1 2 3; do
    curl -s -o /dev/null -m 10 "http://localhost:$TARGET_PORT$HEALTH_PATH"
done

# ── 5. nginx 트래픽 전환 ────────────────────────────────────
echo "🔄 Nginx 트래픽을 $TARGET_COLOR 로 전환합니다."
cp "$ENV_FILE" "$ENV_FILE.bak"
echo "set \$service_url app-$TARGET_COLOR:8080;" > "$ENV_FILE"

# ── 5-a. 호스트에 쓴 내용이 nginx 컨테이너에 실제로 보이는지 확인 ──
# 2026-08-26 장애의 원인: 호스트에서 nginx 디렉터리를 mv 로 교체했더니
# bind mount 가 컨테이너 기동 시점의 inode 를 계속 붙잡고 있어서,
# 여기서 쓴 파일이 nginx 에게 영원히 보이지 않았다. 색 전환이 조용히 무효가 되고,
# 나중에 구버전을 내리는 순간 502 가 됐다. 그 침묵을 여기서 깬다.
if ! $COMPOSE exec -T nginx grep -q "app-$TARGET_COLOR" /etc/nginx/conf.d/service-env.inc; then
    echo "❌ 치명적: 호스트의 $ENV_FILE 을 고쳤는데 nginx 컨테이너에는 반영되지 않았습니다."
    echo "   bind mount 가 낡은 inode 를 보고 있을 가능성이 높습니다. 확인 방법:"
    echo "     stat -c %i $(dirname "$ENV_FILE")"
    echo "     docker compose -f docker-compose.yml exec -T nginx stat -c %i /etc/nginx/conf.d"
    echo "   두 inode 가 다르면 nginx 컨테이너를 재생성해야 합니다."
    echo "   트래픽은 그대로 $OLD_COLOR 에 둡니다 (서비스 영향 없음)."
    mv "$ENV_FILE.bak" "$ENV_FILE"
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi

if ! $COMPOSE exec -T nginx nginx -t; then
    echo "❌ nginx 설정 검증 실패. 원래 설정으로 롤백합니다."
    mv "$ENV_FILE.bak" "$ENV_FILE"
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi

if ! $COMPOSE exec -T nginx nginx -s reload; then
    echo "❌ nginx reload 실패. 원래 설정으로 롤백합니다."
    mv "$ENV_FILE.bak" "$ENV_FILE"
    $COMPOSE exec -T nginx nginx -s reload || true
    $COMPOSE stop -t 10 "app-$TARGET_COLOR"
    exit 1
fi
rm -f "$ENV_FILE.bak"
echo "✅ Nginx 트래픽 전환 완료!"

# ── 6. 드레인 ───────────────────────────────────────────────
# reload 는 즉시 끝나지만, 기존 워커 프로세스는 아직 살아서
# 이미 맺어진 keep-alive 커넥션의 요청을 "옛날 설정"으로 보냅니다.
# 여기서 안 기다리고 구버전을 죽이면 그 요청들이 전부 502 입니다.
echo "⏳ 기존 커넥션 드레인 대기 (${DRAIN_SECONDS}초)..."
sleep "$DRAIN_SECONDS"

# ── 7. 구버전 graceful 종료 ─────────────────────────────────
echo "🛑 기존 $OLD_COLOR 컨테이너를 종료합니다 (최대 ${STOP_TIMEOUT}초 대기)."
$COMPOSE stop -t "$STOP_TIMEOUT" "app-$OLD_COLOR"
$COMPOSE rm -f "app-$OLD_COLOR"

echo "🎉 배포 완료. 현재 서비스 중: $TARGET_COLOR"
