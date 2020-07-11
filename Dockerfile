FROM openjdk:14-alpine
COPY build/libs/trivia-*-all.jar trivia.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "trivia.jar"]