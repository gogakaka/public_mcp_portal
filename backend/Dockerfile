# 멀티 스테이지 빌드: Eclipse Temurin JDK 21
# 1단계: 빌드
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle 래퍼 및 빌드 파일 복사 (의존성 캐시 활용)
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gradlew ./
RUN chmod +x gradlew

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안: root 외 사용자로 실행
RUN addgroup -S umg && adduser -S umg -G umg

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 사용자 변경
USER umg

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx1024m -XX:+UseContainerSupport"

# 가상 스레드 활성화 및 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
