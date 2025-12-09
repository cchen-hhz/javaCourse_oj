FROM openjdk:17-jdk-slim

WORKDIR /app

COPY run-compile.sh /app/run-compile.sh
RUN chmod +x /app/run-compile.sh

# entrypoint 在 Java 里用 --entrypoint 覆盖为 /app/run-compile.sh
