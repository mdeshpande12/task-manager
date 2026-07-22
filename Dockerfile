# Build stage
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# Runtime stage
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/task-manager-1.0-SNAPSHOT.jar app.jar
COPY config.yml config.yml

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "server", "config.yml"]
