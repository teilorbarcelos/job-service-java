### Build stage ###
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp -DskipTests package

### Runtime stage ###
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1001 jobservice
USER jobservice

COPY --from=build /build/target/quarkus-app /app/quarkus-app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-app/quarkus-run.jar"]
