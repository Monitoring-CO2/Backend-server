# Monitoring CO2 - Server

The program is a [Gradle](https://github.com/gradle/gradle) project, developed in Java version 17.

![Workflow badge](https://github.com/Monitoring-CO2/Backend-server/actions/workflows/PackageServer.yml/badge.svg?branch=main)  
![Workflow badge](https://github.com/Monitoring-CO2/Backend-server/actions/workflows/JavaPublish.yml/badge.svg?branch=main)

Click [here](https://github.com/Monitoring-CO2/Backend-server/releases/tag/latest) to download latest .jar release.

## Libraries used

- [Spring Boot](https://github.com/spring-projects/spring-boot)
- [Spring WebFlux](https://github.com/spring-projects/spring-framework)
- [Spring Security](https://github.com/spring-projects/spring-security)
- [Spring Validation](https://github.com/spring-projects/spring-framework)
- [Spring Data MongoDB](https://github.com/spring-projects/spring-data-mongodb)
- [Thymeleaf](https://github.com/thymeleaf)
- [InfluxDB Client Java](https://github.com/influxdata/influxdb-client-java)
- [Jackson](https://github.com/FasterXML/jackson)

## Overall operation of the program

This program is a web server for the Monitoring-CO2 website.  
It is also used as a REST API for integration with the TTN backend (integrated as a [Webhook](https://www.thethingsindustries.com/docs/integrations/webhooks/)).

Its main purposes are :
- Serve web pages to users
- Receive join messages from TTN when a device is started and schedule a downlink to said device with the current real world time.
- Receive downlinks from TTN with payloads when devices send their sensors' data

The server uses two databases :
- [MongoDB](https://www.mongodb.com/) : store the user accounts (to add/delete devices) and devices informations (like ID, DevEUI)
- [InfluxDB](https://www.influxdata.com/) : store the sensors' values for all devices

## Server setup *(required)*

In order to run the server, you have to create an **application.properties** file.

Fill the file with the following :
```properties
#General config
spring.application.name=Monitoring-CO2
#Server config
server.port=8080


#Monitoring CO2 config
monitoringco2.admin.defaultusername=
monitoringco2.admin.defaultpassword=


#MongoDB
spring.data.mongodb.host=
spring.data.mongodb.port=27017
#spring.data.mongodb.username=
#spring.data.mongodb.password=
spring.data.mongodb.database=Monitoring-CO2

#TheThingsNetwork
ttn.webhookId=
ttn.apiKey=
ttn.downlinkApiKey=

#InfluxDB
influxdb.token=
influxdb.bucket=Monitoring CO2 Data
influxdb.org=Monitoring CO2
influxdb.url=
```
Please complete every line after the `=` sign (except the MongoDB username and password which may not be necessary) :
- **server.port** : listening port of the server
- **monitoringco2.admin.defaultusername** : Default administrator account username
- **monitoringco2.admin.defaultpassword** : Default administrator account password
- **spring.data.mongodb.host** : MongoDB database address 
- **spring.data.mongodb.port** : MongoDB database port
- **spring.data.mongodb.database** : Name of the collection to use in the database
- **ttn.webhookId** : ID of the TTN Webhook
- **ttn.apiKey** : TTN API Key (used to schedule downlinks)
- **ttn.downlinkApiKey** : An additionnal key that TTN will send to server (making sure that only TTN can make API calls to the server)
- **influxdb.token** : A token to access (read/write) to the InfluxDB database
- **influxdb.bucket** : Name of the InfluxDB database bucket
- **influxdb.org** : Name of the InfluxDB database organisation
- **influxdb.url** : Address of the InfluxDB database

## Building the server

In order to build the server, make sure you have installed the JDK version 17.

1. Clone the GitHub repo and open a terminal at the root of the project
2. Build the server with the following command :
   - Windows : `./gradlew.bat assemble`
   - Linux : `./gradlew assemble`
3. The compiled .jar is in the `build/libs` folder under the name `Monitoring-CO2-server.jar`

## Starting the server

Regroup in the same folder your `Monitoring-CO2-server.jar` file and your configuration file `application.properties`

1. Open a terminal in the folder containing both files
2. Start the server with the command : `java -jar Monitoring-CO2-server.jar -Dspring.config.location=application.properties`

## Structure of the server code

### Folders
The web server is divided in a few directories.
- `src/main/java` : contains the server's source files
- `src/resources` : contains the server's resources
  - `src/resources/static` : contains static files (like images, CSS, JavaScript) that can be accessed base on their path relative to this folder  
  For example, you can access `favicon.ico` just by going to `http://localhost:8080/favicon.ico`
  - `src/resources/templates` : contains web pages templates  
  For example when you go to `http://localhost:8080/devices`, the web server loads the `list_devices.html` file (based on [WebController](src/main/java/fr/polytech/monitoringco2server/controllers/WebController.java)) and changes its content using the Thymeleaf Template Engine ([more info](https://www.thymeleaf.org/)) before sending it to the user
  - `src/resources/application.properties` : contains various configuration, this is an internal file it **must not be modified** you will need to create another configuration file when configuring the server

### Source code

The main sources files are :  
*(relative to `src/main/java/fr/polytech/monitoringco2server/`)*
- `config/`
  - `InfluxDBConfig.java` : Configures the InfluxDB Client
  - `WebClientConfig.java` : Configures the TTN WebClient (used to make POST requests to TTN)
  - `WebConfig.java` : Configures the Thymeleaf Template Engine for the web server
- `controllers/`
  - `APIController.java` : Contains the REST API code, process requests to various endpoints (`/devices/join`, `/devices/uplink`)  
  Those endpoints are called by TTN which makes requests when it receives various events for a device (like a join or an uplink message)
  - `WebAPIController.java` : Contains a REST API code, used by various JavaScript scripts on some web pages
  - `WebController.java` : Contains the code which serves the web pages
- `database/`
  -  `documents/`
      - `Account.java` : Account class
      - `Device.java` : Device class
  - `repositories/`
    - `AccountRepository.java` : Functions to access the Account collection in the MongoDB database
    - `DeviceRepository.java` : Functions to access the Device collection in the MongoDB database
  - `DatabaseInit.java` : Initialise the MongoDB database with the Administrator account
- `LoRa/`
  - `Downlink.java` : Contain the function which make a POST request to TTN in order to schedule a downlink with the current real world time.
  - `PayloadDecoder.java` : Contains the function which parses the received payload from a device's uplink message. This function will read the payload, decode the sensor's values according to the [Payload Format](https://github.com/Monitoring-CO2/.github/blob/main/profile/README.md#payload-format) and store them in the InfluxDB database.
- `security/`
  - `SecurityConfig.java` : Contains information about which endpoints needs to be secure (require a username/password to proceed)
  - `SecurityUserDetailsService.java` : Configures Spring Security
  - `UserAuthorities.java` : Configures user permissions
  - `UserRoles.java` : Configures user roles
- `MonitoringCo2ServerApplication.java` : Main class and function, starts the Spring server