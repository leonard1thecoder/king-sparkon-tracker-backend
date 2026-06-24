# syntax=docker/dockerfile:1.7
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY .mvn .mvn
COPY src src
RUN --mount=type=secret,id=app_env,required=false \
    if [ -f /run/secrets/app_env ]; then \
      set -a; \
      . /run/secrets/app_env; \
      set +a; \
    fi; \
    mvn -B clean verify

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
