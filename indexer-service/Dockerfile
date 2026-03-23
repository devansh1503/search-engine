FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY indexer-service/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]