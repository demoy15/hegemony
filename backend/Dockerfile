# syntax=docker/dockerfile:1.7
FROM gradle:8.11.1-jdk21 AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    gradle dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENV HEGEMONY_SAVE_DIR=/app/data/saves
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
