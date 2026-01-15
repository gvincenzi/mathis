# Usa una immagine Java runtime leggera
FROM eclipse-temurin:17-jre as runtime

# Copia il JAR costruito da Maven nel container
COPY target/*.jar gist-mathis.jar

# Comando di default per eseguire l'applicazione
ENTRYPOINT ["java", "-jar", "/gist-mathis.jar"]
