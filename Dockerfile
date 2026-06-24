FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
RUN apt-get update -qq \
    && apt-get install -y -qq --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn -q -B -DskipTests -Dstyle.color=never dependency:go-offline
COPY src src
RUN set +e; \
    mvn -q -B -DskipTests -Dstyle.color=never package > /tmp/maven-package.log 2>&1; \
    status=$?; \
    if [ "$status" -ne 0 ]; then \
      echo "--- Maven package failure tail ---"; \
      tail -n 120 /tmp/maven-package.log; \
    fi; \
    exit "$status"

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
