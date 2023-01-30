package fr.polytech.monitoringco2server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
class MonitoringCo2ServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
