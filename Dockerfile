FROM openjdk:jdk

WORKDIR /usr/app

COPY build/libs/Monitoring-CO2-server-0.0.1-SNAPSHOT.jar .
#COPY src/application.properties .

CMD ["java", "-jar", "Monitoring-CO2-server-0.0.1-SNAPSHOT.jar", "-Dspring.config.location=application.properties"]
