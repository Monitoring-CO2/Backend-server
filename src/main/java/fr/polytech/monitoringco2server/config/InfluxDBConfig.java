package fr.polytech.monitoringco2server.config;

import com.influxdb.client.reactive.InfluxDBClientReactive;
import com.influxdb.client.reactive.InfluxDBClientReactiveFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfig {

	@Value("${influxdb.url}")
	private String databaseUrl;

	@Value("${influxdb.token}")
	private String databaseToken;

	@Value("${influxdb.bucket}")
	private String databaseBucket;

	@Value("${influxdb.org}")
	private String databaseOrg;

	@Bean
	public InfluxDBClientReactive buildConnection(){
		return InfluxDBClientReactiveFactory.create(databaseUrl, databaseToken.toCharArray(), databaseOrg, databaseBucket);
	}
}
