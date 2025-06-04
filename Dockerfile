FROM openjdk:21

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

RUN mkdir /resources

EXPOSE 1780
ENTRYPOINT ["java", "-jar", "app.jar"]
