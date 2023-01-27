package fr.polytech.monitoringco2server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Value("${ttn.apiKey}")
	private String TTN_API_KEY;

	@Bean
	public WebClient ttnWebClient(WebClient.Builder webClientBuilder){
		return webClientBuilder
				.baseUrl("https://eu1.cloud.thethings.network")
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(MediaType.APPLICATION_JSON);
					httpHeaders.setBearerAuth(TTN_API_KEY);
				})
				.build();
	}
}
