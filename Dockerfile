# Stage1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# 빌드용 디렉토리 설정
WORKDIR /build

# Gradle wrapper 파일들 먼저 복사 (캐시 최적화)
COPY gradlwe .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmode +x ./gradlew

# 의존성 다운로드 (별도 레이어로 캐시 활용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 제외로 빌드 시간 단축)
RUN ./gradlew bootJar --no-daemon -x test

# Stage2: Runtime stage (경량화된 실행 환경)
FROM eclipse-temurin:21-jre-alpine

# 보안을 위한 비특권 사용자 생성
RUN addgroup --system --gid 1001 membergroup && \
    adduser --system --uid 1001 --ingroup membergroup memberuser

# 애플리케이션 디렉토리 설정
WORKDIR /app

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 빌드된 JAR 파일을 Runtime stage로 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 파일 소유권을 애플리케이션 사용자로 변경
RUN chown memberuser:membergroup app.jar

# 비특권 사용자로 전환
USER memberuser

# 컨테이너가 사용할 포트 노출
EXPOSE 8081

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuato/health || exit 1

# JVM 최적화 옵션과 함께 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=dockeer", \
    "-Xms256m", \
    "-Xms512m", \
    "--enable-preview", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-XX:+OptimizeStringConcat", \
    "-jar", \
    "app.jar"]
