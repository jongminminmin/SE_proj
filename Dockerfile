# ──────────────────────────────────────────────────────────────────────────────
# 1) Build 단계: Gradle로 JAR 생성
# ──────────────────────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS builder
WORKDIR /home/gradle/project

# build 스크립트만 복사 (gradle.properties 제외)
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle gradle

# 소스 코드 복사
COPY src src

# 빌드 (테스트 제외)
RUN ./gradlew clean build -x test

# ──────────────────────────────────────────────────────────────────────────────
# 2) Runtime 단계: 경량 JRE 이미지에 복사
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드된 JAR만 복사
COPY --from=builder /home/gradle/project/build/libs/*SNAPSHOT.jar app.jar

# 외부에 노출할 포트
EXPOSE 9000
EXPOSE 33455

# JVM 옵션
ENV JAVA_OPTS="-Xms512m -Xmx1g"

# 앱 실행
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar --server.port=9000"]
