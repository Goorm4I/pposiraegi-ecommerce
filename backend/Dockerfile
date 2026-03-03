# [Stage 1: Build]
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Dockerfile이 backend 안에 있으므로 상대 경로로 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 라이브러리 캐싱
RUN ./gradlew build -x test --parallel --continue > /dev/null 2>&1 || true

# 소스 복사 (Dockerfile 기준 src 폴더)
COPY src ./src
RUN ./gradlew clean bootJar -x test

# [Stage 2: Run]
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드 결과물 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Seoul -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]