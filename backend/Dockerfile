# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
# Download dependencies first so they are cached between source changes.
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run with `mvnw verify` (see README); skipping them keeps image builds fast.
RUN mvn -B -q -DskipTests package

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 quickfind
WORKDIR /app
COPY --from=build /build/target/quickfind-backend.jar app.jar
USER quickfind
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# Extra arguments (e.g. --spring.profiles.active=generate) are passed through to the app.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar \"$@\"", "--"]
