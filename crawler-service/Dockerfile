FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY crawler-service/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]