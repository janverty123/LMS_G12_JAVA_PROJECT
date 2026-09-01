FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy

RUN useradd --create-home --uid 10001 classify

WORKDIR /app
COPY --from=build --chown=classify:classify /workspace/target/apptitle-backend-0.1.0-SNAPSHOT.jar app.jar

USER classify
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
