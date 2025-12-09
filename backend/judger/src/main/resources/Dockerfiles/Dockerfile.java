FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

RUN mkdir -p /app/code /app/input

COPY run.java.sh /app/run.sh
RUN chmod +x /app/run.sh

CMD ["/app/run.sh"]