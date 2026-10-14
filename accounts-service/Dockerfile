# Contexto de build: la raíz del repositorio (multi-módulo).
#   docker build -f accounts-service/Dockerfile .
FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn --batch-mode --no-transfer-progress -pl accounts-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre

RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/accounts-service/target/accounts-service-*.jar app.jar
USER spring:spring

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
