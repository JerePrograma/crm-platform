FROM maven:3.9.16-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/crm-backend-*.jar /app/app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
  CMD curl --fail --silent http://localhost:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
