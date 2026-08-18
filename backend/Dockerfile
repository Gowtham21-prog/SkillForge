# Use a multi-stage build: build with Maven (Eclipse Temurin 17), run with a lightweight JRE image (Temurin 17)

FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy only the pom first to utilize Docker cache for dependencies
COPY pom.xml ./
# Copy source
COPY src ./src

# Download dependencies and build the application (skip tests for faster builds on Render)
RUN mvn -B -ntp -DskipTests dependency:go-offline
RUN mvn -B -ntp -DskipTests package

# Final runtime image
FROM eclipse-temurin:17-jre

# The Spring Boot maven plugin by default writes the repackaged jar to target/<artifactId>-<version>.jar
ARG JAR_FILE=target/elearning-platform-1.0.0.jar

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /workspace/${JAR_FILE} ./app.jar

# The application is configured to read the PORT environment variable (server.port=${PORT:8080}).
# Render provides PORT automatically; no hard-coded port is required here.
ENV JAVA_OPTS=""

# Use a shell form so optional JAVA_OPTS can be respected by Render.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
