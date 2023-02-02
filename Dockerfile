FROM openjdk:jdk

WORKDIR /usr/app

COPY build/libs/Monitoring-CO2-server.jar .
#COPY src/application.properties .

CMD ["java", "-jar", "Monitoring-CO2-server.jar", "-Dspring.config.location=application.properties"]
