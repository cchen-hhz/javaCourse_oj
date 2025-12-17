FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      g++ make bash coreutils time nsjail ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# build: mvn -q -DskipTests package
# then copy target/judge-worker-1.0.0.jar
COPY target/judge-worker-1.0.0.jar /app/judge-worker.jar

ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_SUBMISSION_TOPIC=submission
ENV KAFKA_RESULT_TOPIC=result
ENV JUDGE_WORKERS=2

CMD ["java","-jar","/app/judge-worker.jar"]
