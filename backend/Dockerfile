# [Stage 1: Build]
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# 모듈 이름을 인자로 받음 (예: api-gateway, user-service)
ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# Gradle Wrapper 및 설정 파일 복사 (루트 기준 경로로 수정)
COPY backend/gradlew .
COPY backend/gradle gradle
COPY backend/build.gradle backend/settings.gradle ./

# 모든 모듈의 build.gradle 복사
COPY backend/api-gateway/build.gradle backend/api-gateway/
COPY backend/common/build.gradle backend/common/
COPY backend/grpc-interface/build.gradle backend/grpc-interface/
COPY backend/order-service/build.gradle backend/order-service/
COPY backend/product-service/build.gradle backend/product-service/
COPY backend/user-service/build.gradle backend/user-service/

# 공통 모듈 소스 복사
COPY backend/common/src backend/common/src
COPY backend/grpc-interface/src backend/grpc-interface/src

# 대상 모듈 소스만 복사
COPY backend/${MODULE_NAME}/src backend/${MODULE_NAME}/src

RUN chmod +x gradlew
# backend/ 폴더 안의 settings.gradle 위치에 맞춰 빌드 실행
RUN ./gradlew -p backend :${MODULE_NAME}:bootJar -x test

# [Stage 2: Run]
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# 빌드된 대상 모듈의 jar만 복사
COPY --from=build /app/backend/${MODULE_NAME}/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Seoul -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
