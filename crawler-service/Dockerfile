FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY crawler-service/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]