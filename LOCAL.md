# 로컬 실행 가이드 (Docker)

로컬은 전부 컨테이너로 띄웁니다. `docker-compose.yml`(배포용 블루/그린 토폴로지)은 손대지 않고,
`docker-compose.local.yml`이 병합되어 로컬용 설정을 덮습니다.

이 파일은 일부러 `docker-compose.override.yml`이 **아닌** 이름입니다. override 라는 이름이면
Compose 가 어디서든 자동 병합하는데, 이 파일은 git 으로 운영 서버에도 내려가기 때문에
서버에서 `docker compose up` 한 번에 로컬 설정이 운영에 적용됩니다.
대신 `.env` 의 `COMPOSE_FILE` 로 로컬에서만 병합합니다 (0번 참고).

**앱 주소: http://localhost:8081**

---

## 0. 사전 준비

```bash
# Docker Desktop 이 꺼져 있으면 먼저 켜기 (터미널에서 ! 붙여 실행)
open -a Docker

# 데몬 올라왔는지 확인
docker info >/dev/null && echo "docker ready"
```

`.env` 에 `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` 이 있어야 합니다. (이미 있음)

그리고 **`.env` 에 아래 한 줄이 반드시 있어야** 아래의 `docker compose ...` 명령들이
로컬 설정으로 동작합니다. 이 줄이 없으면 운영 토폴로지(app-blue+green+nginx, 8080)가 뜹니다.

```
COMPOSE_FILE=docker-compose.yml:docker-compose.local.yml
```

확인:

```bash
docker compose config | grep -E "8081|ounce-api:local"   # 나오면 병합 정상
```

---

## 1. 빌드

Dockerfile 이 단일 스테이지(`COPY target/*-SNAPSHOT.jar`)라서 **jar 를 먼저 만들어야** 합니다.

```bash
./mvnw -DskipTests package        # target/demo-0.0.1-SNAPSHOT.jar 생성
docker compose build app-blue     # jar 를 ounce-api:local 이미지로 굽기
```

코드를 고칠 때마다 위 두 줄을 다시 돌립니다.

---

## 2. 기동

### 기본 (가벼움) — MySQL + Redis + 앱

검색은 JPA 로 동작합니다. Elasticsearch/Kibana/app-green/nginx 는 뜨지 않습니다.

```bash
docker compose up -d
docker compose ps
```

### 검색까지 (Elasticsearch + Kibana)

```bash
SEARCH_ENGINE=elasticsearch docker compose --profile search up -d
```

ES 는 헬스체크가 붙어 있어 `healthy` 될 때까지 40초 정도 걸립니다.
앱이 ES 보다 먼저 뜰 수 있으니 ES 가 `healthy` 된 걸 확인한 뒤 앱을 재시작하는 게 안전합니다:

```bash
docker compose ps elasticsearch          # healthy 확인
docker compose restart app-blue
```

- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

### 블루/그린 + nginx 까지 (배포 구성 재현)

```bash
docker compose --profile bluegreen --profile search up -d
```

nginx 가 80 번에서 `app-blue:8080` 으로 프록시합니다 (`nginx/conf.d/service-env.inc`).

---

## 3. 로그 보기 ⭐

### 포그라운드로 띄워서 전부 흘려보기

```bash
docker compose up          # -d 없이. Ctrl+C 로 종료
```

### 앱 로그만 따라가기 (가장 자주 쓰는 것)

```bash
docker compose logs -f --tail=100 app-blue
```

### 전체 서비스 로그 + 타임스탬프

```bash
docker compose logs -f --tail=50 --timestamps
```

### 특정 서비스만

```bash
docker compose logs -f ounce-db
docker compose logs -f redis
docker compose logs -f elasticsearch
```

### 부팅 성공 시 확인할 줄

정상이면 아래가 순서대로 나옵니다:

```
HikariPool-1 - Start completed.                     ← DB 연결 성공
Tomcat started on port 8080 (http) with context path '/'
Started DemoApplication in 8.4 seconds
```

`Tomcat started` 와 `Started DemoApplication` 이 안 보이면 그 위 스택트레이스가 원인입니다.

### 로그 필터링 팁

```bash
# 에러만
docker compose logs app-blue | grep -iE "error|exception|caused by"

# SQL 소음 빼고 보기
docker compose logs app-blue | grep -v "Hibernate:"

# SQL 을 보고 싶을 때 (기본은 WARN 으로 눌러놨음)
SQL_LOG=DEBUG docker compose up -d app-blue
```

로그 레벨은 `docker-compose.local.yml` 의 `LOGGING_LEVEL_*` 환경변수로 조절합니다.
`local` 프로필이 `show_sql`/`format_sql`/`use_sql_comments` 를 다 켜두기 때문에
기본값으로 `org.hibernate.SQL` 을 `WARN` 으로 눌러놨습니다.

---

## 4. 정리 / 재시작

```bash
docker compose restart app-blue      # 앱만 재시작
docker compose down                  # 컨테이너만 내리기 (데이터 유지)
docker compose down -v               # ⚠️ 볼륨까지 삭제 = DB 데이터 전부 날아감
```

---

## 5. 자주 걸리는 것들

| 증상 | 원인 / 해결 |
|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop 이 꺼짐 → `open -a Docker` |
| `COPY target/*-SNAPSHOT.jar: no such file` | jar 를 안 만듦 → `./mvnw -DskipTests package` |
| `Access denied for user 'root'` | `db_data` 볼륨이 옛 비밀번호로 초기화됨. MySQL 은 첫 기동 때만 `MYSQL_ROOT_PASSWORD` 를 씀 → `docker compose down -v` (⚠️ DB 데이터 삭제) |
| `port is already allocated` (6379) | 호스트 brew redis 가 점유 중. `docker-compose.local.yml` 에서 6380 으로 옮겨놨으니 그대로 두면 됨. 다른 포트가 충돌하면: `lsof -i :3306 -i :8081 -i :9200 -sTCP:LISTEN` |
| 앱이 8081 이 아니라 8080 에 뜬다 / `app-green`·`nginx` 가 같이 뜬다 | `.env` 의 `COMPOSE_FILE` 이 없어서 `docker-compose.local.yml` 이 병합되지 않음 → 0번 참고 |
| Redis 연결 실패 (`localhost/<unresolved>:6379`) | Boot 4 는 `spring.data.redis.*` 를 읽습니다. `SPRING_REDIS_HOST` 는 Boot 2 이름이라 **에러 없이 무시**되고 localhost 로 붙습니다. `SPRING_DATA_REDIS_HOST` 가 정답 (2026-08-26 운영 502 장애 원인) |
| ES 연결 실패 | `SEARCH_ENGINE=elasticsearch` 인데 `--profile search` 를 안 붙임 |
| ES 이미지 pull 실패 | `9.4.3` 태그 확인. `Dockerfile.es` 가 nori 플러그인을 설치하므로 첫 빌드가 느림 |

---

## 6. 호스트에서 직접 실행 (IntelliJ)

인프라만 컨테이너로 띄우고 앱은 IDE 에서 돌리는 방식:

```bash
docker compose up -d ounce-db redis
```

이때 앱은 `application-local.yml` 의 `localhost` 값을 그대로 씁니다.
단 Redis 호스트 포트가 6380 이라 IDE 실행 시엔 brew redis(6379)를 쓰거나
`-Dspring.data.redis.port=6380` 을 넘기세요.
**그래서 yml 을 컨테이너 호스트명으로 고치지 않았습니다.**
