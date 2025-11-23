# 1. 빌드 스테이지
FROM gradle:8-jdk17-alpine AS bootbuild
WORKDIR /website/backend

# Gradle 설정 파일 복사 후 디펜던시 설정
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스 코드 복사 후 빌드
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# 2. 런타임 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /website/backend

# Alpine 리눅스에 타임존 데이터(tzdata) 설치 및 서울 시간 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 빌드 스테이지에서 생성한 JAR 파일을 런타임 스테이지로 복사
# Gradle 프로젝트의 빌드 디렉터리는 build임
COPY --from=bootbuild /website/backend/build/libs/*.jar website.jar

# 포트 노출 (부트 기본 포트)
EXPOSE 8080

# JAR 파일 실행
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "website.jar"]