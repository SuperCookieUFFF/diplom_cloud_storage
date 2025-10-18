FROM openjdk:17-jdk-slim

WORKDIR /app


COPY target/file-service-*.jar app.jar


RUN mkdir -p /app/logs /app/uploads


EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]