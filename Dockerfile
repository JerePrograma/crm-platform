FROM maven:3.9.16-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
RUN --mount=type=cache,id=crm-maven-java21,target=/root/.m2,sharing=locked \
    mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
COPY deploy/RuntimeHealthCheck.java /workspace/healthcheck/RuntimeHealthCheck.java
RUN --mount=type=cache,id=crm-maven-java21,target=/root/.m2,sharing=locked \
    mvn -B -DskipTests clean package \
    && javac --release 21 -d /workspace/healthcheck /workspace/healthcheck/RuntimeHealthCheck.java

FROM eclipse-temurin:21-jre-alpine@sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/crm-backend-*.jar /app/app.jar
COPY --from=backend-build /workspace/healthcheck/RuntimeHealthCheck.class /app/healthcheck/RuntimeHealthCheck.class
USER 65532:65532
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
  CMD ["java", "-cp", "/app/healthcheck", "RuntimeHealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
