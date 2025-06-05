FROM openjdk:21

WORKDIR /api_gateway

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 1780

ENTRYPOINT ["java", "-jar", "app.jar"]
