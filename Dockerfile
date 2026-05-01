# [Stage 1: Build]
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# 프로젝트 루트 기준 경로 복사
COPY backend/gradlew backend/
COPY backend/gradle backend/gradle
COPY backend/build.gradle backend/settings.gradle backend/

# 모든 모듈의 build.gradle 복사 (사용자 수정본 반영: 세분화된 common 모듈)
COPY backend/api-gateway/build.gradle backend/api-gateway/
COPY backend/common/common-core/build.gradle backend/common/common-core/
COPY backend/common/common-jpa/build.gradle backend/common/common-jpa/
COPY backend/common/common-web-mvc/build.gradle backend/common/common-web-mvc/
COPY backend/grpc-interface/build.gradle backend/grpc-interface/
COPY backend/order-service/build.gradle backend/order-service/
COPY backend/product-service/build.gradle backend/product-service/
COPY backend/user-service/build.gradle backend/user-service/

# 공통 모듈 및 소스 복사 (사용자 수정본 반영)
COPY backend/common/common-core/src backend/common/common-core/src
COPY backend/common/common-jpa/src backend/common/common-jpa/src
COPY backend/common/common-web-mvc/src backend/common/common-web-mvc/src
COPY backend/grpc-interface/src backend/grpc-interface/src

# 대상 모듈 소스만 복사
COPY backend/${MODULE_NAME}/src backend/${MODULE_NAME}/src

RUN chmod +x backend/gradlew
# backend 폴더를 명시하여 빌드
RUN ./backend/gradlew -p backend :${MODULE_NAME}:bootJar -x test

# [Stage 2: Run]
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

# 빌드된 대상 모듈의 jar만 복사
COPY --from=build /app/backend/${MODULE_NAME}/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Seoul -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
