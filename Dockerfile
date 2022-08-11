FROM maven AS builder
COPY . .
RUN mvn clean verify

FROM openjdk:17-jdk-slim
COPY --from=builder target/jmxClient-shaded.jar .
CMD ["java", "-jar", "target/jmxClient-shaded.jar"]
