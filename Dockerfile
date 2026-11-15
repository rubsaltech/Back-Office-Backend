# ---- Build stage: compile the Spring Boot fat JAR ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first (only re-downloads when pom.xml changes).
COPY pom.xml .
RUN mvn -q -ntp dependency:go-offline

# Then build the app.
COPY src ./src
RUN mvn -q -ntp -DskipTests clean package

# ---- Runtime stage: slim JRE with just the JAR ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Uploaded files land here; mounted to a Docker volume in compose so they persist.
RUN mkdir -p /app/uploads
ENV UPLOADS_DIR=/app/uploads
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
# Flyway runs the migrations automatically on startup (baseline-on-migrate).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
