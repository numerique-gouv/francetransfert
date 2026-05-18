# syntax=docker/dockerfile:1.6
# Build context: repo root.

FROM maven:3-eclipse-temurin-25 AS builder
WORKDIR /build

COPY francetransfert-core/pom.xml francetransfert-core/pom.xml
COPY francetransfert-core/src francetransfert-core/src
RUN --mount=type=cache,target=/root/.m2 \
    cd francetransfert-core && mvn -B -DskipTests install

COPY francetransfert-upload-api/pom.xml francetransfert-upload-api/pom.xml
COPY francetransfert-upload-api/src francetransfert-upload-api/src
RUN --mount=type=cache,target=/root/.m2 \
    cd francetransfert-upload-api && mvn -B -DskipTests -P prod package

FROM gcr.io/distroless/java25:nonroot
WORKDIR /app
COPY --from=builder /build/francetransfert-upload-api/target/*.jar /app/app.jar
ENV TZ=Europe/Paris
EXPOSE 8080
CMD ["-jar", "/app/app.jar"]
