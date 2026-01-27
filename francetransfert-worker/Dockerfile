FROM gcr.io/distroless/java25:nonroot

WORKDIR /app
COPY target/*.jar /app/app.jar

ENV TZ=Europe/Paris

# Lancer le script de démarrage
CMD ["-jar", "/app/app.jar"]