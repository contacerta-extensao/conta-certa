FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw package -DskipTests && \
    cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache openssl && \
    addgroup -S contacerta && \
    adduser -S contacerta -G contacerta

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

RUN chmod +x /usr/local/bin/docker-entrypoint.sh && \
    mkdir -p /app/keys && \
    chown -R contacerta:contacerta /app

USER contacerta

EXPOSE 8080

ENTRYPOINT ["docker-entrypoint.sh"]
