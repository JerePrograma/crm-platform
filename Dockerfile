FROM maven:3.9.16-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
COPY deploy/RuntimeHealthCheck.java /workspace/healthcheck/RuntimeHealthCheck.java
RUN mvn -B -DskipTests package \
    && javac --release 21 -d /workspace/healthcheck /workspace/healthcheck/RuntimeHealthCheck.java

FROM cgr.dev/chainguard/jre@sha256:553fa376d9ac5d23912a87bd6352acacbf96a08ba04202abbf16fa2bc02fdf14
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/crm-backend-*.jar /app/app.jar
COPY --from=backend-build /workspace/healthcheck/RuntimeHealthCheck.class /app/healthcheck/RuntimeHealthCheck.class
USER 65532:65532
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
  CMD ["java", "-cp", "/app/healthcheck", "RuntimeHealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
