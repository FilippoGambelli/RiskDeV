FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y maven

CMD ["mvn", "spring-boot:run"]
