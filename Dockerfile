FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY backend .

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

EXPOSE 8080

CMD ["java","-jar","build/libs/dduk-backend-0.0.1-SNAPSHOT.jar"]