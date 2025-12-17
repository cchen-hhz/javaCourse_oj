# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install runtime dependencies (g++ for compiling C++, bash for shell execution)
RUN apk add --no-cache g++ bash

# Copy the jar from the builder stage
COPY --from=builder /app/target/judge-worker-1.0.0.jar /app/judge-worker.jar

# Set environment variables
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_SUBMISSION_TOPIC=submission-queue
ENV KAFKA_RESULT_TOPIC=judge-result

# Run the application
CMD ["java", "-jar", "/app/judge-worker.jar"]
