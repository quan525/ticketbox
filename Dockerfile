FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
RUN npm run build

FROM gradle:9.2.1-jdk17-alpine AS builder
WORKDIR /workspace
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src
COPY --from=frontend /frontend/dist ./frontend/dist
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S ticketbox && adduser -S ticketbox -G ticketbox
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER ticketbox
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
