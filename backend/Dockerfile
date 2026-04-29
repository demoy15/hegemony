FROM gradle:8.11.1-jdk21 AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENV HEGEMONY_SAVE_DIR=/app/data/saves
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
