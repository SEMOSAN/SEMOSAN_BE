FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/api-*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
