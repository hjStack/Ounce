FROM eclipse-temurin:21-jdk

# 1. 작업할 폴더(방)를 명시적으로 지정합니다. (이걸 안 하면 가끔 경로가 꼬입니다)
WORKDIR /app

# 2. target 폴더 안에 있는 빌드 결과물(jar)을 app.jar라는 이름으로 복사합니다.
# 팁: 프로젝트 이름이 demo인지 ounce인지 헷갈릴 때를 대비해 *-SNAPSHOT.jar 로 쓰면 찰떡같이 찾아서 복사합니다!
COPY target/*-SNAPSHOT.jar app.jar

# 3. 복사된 app.jar를 실행합니다. (WORKDIR 안이므로 /app.jar가 아니라 그냥 app.jar 입니다)
ENTRYPOINT ["java", "-jar", "app.jar"]