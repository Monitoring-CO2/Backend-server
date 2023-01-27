package fr.polytech.monitoringco2server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class MonitoringCo2ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitoringCo2ServerApplication.class, args);
	}

}
