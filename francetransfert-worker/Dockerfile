FROM bitnami/java:21
COPY target/*.jar app.jar

# Lancer le script de démarrage
ENTRYPOINT ["java", "-jar", "/app.jar"]