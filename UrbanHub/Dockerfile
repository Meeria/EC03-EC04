FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY gradlew package.json package-lock.json settings.gradle build.gradle ./
COPY gradle gradle

RUN chmod +x gradlew
RUN ./gradlew wrapper --no-daemon || true

COPY src ./src

EXPOSE 8080