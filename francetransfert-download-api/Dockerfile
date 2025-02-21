FROM bitnami/java:21
WORKDIR /app
COPY target/*.jar /app/app.jar

# Lancer le script de démarrage
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EXPOSE 8081