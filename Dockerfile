# =====================================================================
# Backend Dockerfile - Board of Revenue e-Board
# Multi-stage: Maven build -> lightweight JRE runtime
# =====================================================================

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r eboard && useradd -r -g eboard eboard \
    && mkdir -p /app/uploads /app/logs \
    && chown -R eboard:eboard /app

COPY --from=build /build/target/eboard-backend-1.0.0.jar app.jar

USER eboard
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
