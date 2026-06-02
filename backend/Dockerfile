# Build Stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# COPY gradle wrapper first for caching
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle/ gradle/

# Ensure gradlew has execution permission
RUN chmod +x gradlew

# Download dependencies to cache them
RUN ./gradlew dependencies --no-daemon

# Copy source and build bootJar
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Setup environment
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -Dspring.profiles.active=prod -jar app.jar"]
