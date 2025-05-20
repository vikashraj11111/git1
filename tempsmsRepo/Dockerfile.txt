FROM openjdk:17

WORKDIR C:\git1\tempsmsRepo

COPY Target1/tempsms-3.1.2.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]