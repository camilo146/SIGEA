# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd

# Prefetch de dependencias/plugins con reintentos para reducir fallos transitorios de red.
RUN sh -c 'for i in 1 2 3 4 5; do mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=8 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 dependency:go-offline && exit 0; echo "Maven prefetch failed (attempt $i), retrying..."; sleep 10; done; exit 1'

COPY src src
RUN sh -c 'for i in 1 2 3 4 5; do mvn -B -ntp -Dmaven.test.skip=true -Dmaven.wagon.http.retryHandler.count=8 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 clean package && exit 0; echo "Maven build failed (attempt $i), retrying..."; sleep 10; done; exit 1'

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/target/sigea-backend-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
