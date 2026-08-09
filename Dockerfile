# Stage1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# 빌드용 디렉토리 설정
WORKDIR /build

# Gradle wrapper 파일들 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmod +x ./gradlew

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

# 프로필 이미지 업로드 디렉터리 생성 (app.image.upload-dir=images 기준, /app/images/profile)
# /app 은 root 소유이므로 비특권 유저가 쓸 수 있도록 미리 생성한다.
RUN mkdir -p /app/images/profile

# 파일/디렉터리 소유권을 애플리케이션 사용자로 변경
RUN chown -R memberuser:membergroup app.jar /app/images

# 비특권 사용자로 전환
USER memberuser

# 컨테이너가 사용할 포트 노출 (배포 플랫폼이 PORT 를 주입하면 그 값으로 바인딩된다)
EXPOSE 8083

# 활성 프로필. -D 시스템 프로퍼티로 고정하면 배포 플랫폼의 환경변수로 덮어쓸 수 없으므로
# ENV 로 기본값만 주고, Railway 등에서는 SPRING_PROFILES_ACTIVE=prod 로 재정의한다.
ENV SPRING_PROFILES_ACTIVE=docker

# 헬스체크 설정 (PORT 주입 시 그 포트를 검사)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8083}/actuator/health || exit 1

# JVM 최적화 옵션과 함께 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-XX:+OptimizeStringConcat", \
    "-jar", \
    "app.jar"]
