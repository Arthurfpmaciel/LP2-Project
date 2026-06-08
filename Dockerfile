FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /app/data
COPY --from=build /workspace/target/agent-manager-0.0.1-SNAPSHOT.jar /app/agent-manager.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/agent-manager.jar"]
