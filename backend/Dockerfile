# [Stage 1: Build]
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# 모듈 이름을 인자로 받음 (예: api-gateway, user-service)
ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 모든 모듈의 build.gradle 복사 (캐시 활용 최적화)
COPY api-gateway/build.gradle api-gateway/
COPY common/build.gradle common/
COPY grpc-interface/build.gradle grpc-interface/
COPY order-service/build.gradle order-service/
COPY product-service/build.gradle product-service/
COPY user-service/build.gradle user-service/

# 공통 모듈 소스 복사 (항상 필요)
COPY common/src common/src
COPY grpc-interface/src grpc-interface/src

# 대상 모듈 소스만 복사
COPY ${MODULE_NAME}/src ${MODULE_NAME}/src

RUN chmod +x gradlew
# 해당 모듈만 빌드 (의존하는 common 등은 같이 빌드됨)
RUN ./gradlew :${MODULE_NAME}:bootJar -x test

# [Stage 2: Run]
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# 빌드된 대상 모듈의 jar만 복사
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Seoul -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
