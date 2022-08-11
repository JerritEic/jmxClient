FROM maven AS builder
COPY . .
RUN mvn clean verify

FROM openjdk:17-jdk-alpine
COPY --from=builder target/jmxClient-shaded.jar /app/
RUN mkdir /data
CMD ["java", "-jar", "/app/jmxClient-shaded.jar"]