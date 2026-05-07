FROM eclipse-temurin:21-jre AS builder
WORKDIR /app
COPY build/libs/api-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "org.springframework.boot.loader.launch.JarLauncher"]
