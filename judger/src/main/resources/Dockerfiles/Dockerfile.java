FROM amazoncorretto:21-alpine-jdk
ARG SUBMISSION_ID
WORKDIR /app
RUN mkdir -p /app/code
COPY data/submission/${SUBMISSION_ID}/code.java /app/code/Main.java
RUN javac /app/code/Main.java
CMD ["java", "-cp", "/app/code", "Main"]