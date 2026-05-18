# syntax=docker/dockerfile:1.6
# Build context: repo root.

FROM maven:3-eclipse-temurin-25 AS builder
WORKDIR /build

COPY francetransfert-core/pom.xml francetransfert-core/pom.xml
COPY francetransfert-core/src francetransfert-core/src
RUN --mount=type=cache,target=/root/.m2 \
    cd francetransfert-core && mvn -B -DskipTests install

COPY francetransfert-download-api/pom.xml francetransfert-download-api/pom.xml
COPY francetransfert-download-api/src francetransfert-download-api/src
RUN --mount=type=cache,target=/root/.m2 \
    cd francetransfert-download-api && mvn -B -DskipTests -P prod package

FROM gcr.io/distroless/java25:nonroot
WORKDIR /app
COPY --from=builder /build/francetransfert-download-api/target/*.jar /app/app.jar
ENV TZ=Europe/Paris
EXPOSE 8081
CMD ["-jar", "/app/app.jar"]
