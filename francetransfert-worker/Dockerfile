FROM bitnamilegacy/java:21

RUN mkdir -p /app
WORKDIR /app
COPY target/*.jar /app/app.jar

ENV TZ Europe/Paris

# Lancer le script de démarrage
ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar