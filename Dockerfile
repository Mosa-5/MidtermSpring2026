# Build stage: compile, run tests, package the jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package

# Runtime stage: JRE only, runs the packaged jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/uno-cli-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
