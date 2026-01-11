FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar mathis.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "mathis.jar"]