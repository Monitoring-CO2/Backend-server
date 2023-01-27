package fr.polytech.monitoringco2server.LoRa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.polytech.monitoringco2server.database.repositories.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

@Component
public class Downlink {

	private static final Logger logger = LoggerFactory.getLogger("Downlink.java");

	@Value("${ttn.webhookId}")
	private String TTN_WEBHOOK_ID;

	final WebClient ttnWebClient;
	final DeviceRepository deviceRepository;

	public Downlink(WebClient ttnWebClient, DeviceRepository deviceRepository) {
		this.ttnWebClient = ttnWebClient;
		this.deviceRepository = deviceRepository;
	}

	public Mono<ResponseEntity<String>> downlinkTimeSync(byte[] data) {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode globalOjb;
		try {
			globalOjb = objectMapper.readTree(data);
			String applicationId = globalOjb.get("end_device_ids").get("application_ids").get("application_id").asText();
			String deviceId = globalOjb.get("end_device_ids").get("device_id").asText();
			String url = "/api/v3/as/applications/"+applicationId+"/webhooks/"+TTN_WEBHOOK_ID+"/devices/"+deviceId+"/down/replace";

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode rootNode = mapper.createObjectNode();
			ArrayNode downlinks = mapper.createArrayNode();
			ObjectNode downlink = mapper.createObjectNode();
			ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
			byteBuffer.putLong(System.currentTimeMillis()/1000L);
			byte[] payload = byteBuffer.array();
			int zeroes = 0;
			while (payload[zeroes] == 0) {
				++zeroes;
			}
			downlink.put("frm_payload", Base64.getEncoder().encodeToString(Arrays.copyOfRange(payload, zeroes, payload.length)));
			downlink.put("f_port", 2);
			downlink.put("confirmed", true);
			downlink.put("priority", "NORMAL");
			downlinks.add(downlink);
			rootNode.set("downlinks", downlinks);

			return ttnWebClient.post()
					.uri(url)
					.body(Mono.just(mapper.writeValueAsString(rootNode)), String.class)
					.retrieve().toEntity(String.class)
					.onErrorResume(throwable -> {
						logger.warn("Error when requesting downlink to TTN for "+deviceId, throwable);
						return Mono.just(new ResponseEntity<>("Error when requesting downlink to TTN : " + throwable, HttpStatus.INTERNAL_SERVER_ERROR));
					})
					.flatMap(response -> {
						logger.debug("Scheduled time downlink for device "+deviceId);
						return deviceRepository.findByDeviceIdIs(deviceId).flatMap(device -> {
							if(device == null){
								return Mono.just(new ResponseEntity<>("OK but device not registered in database !", HttpStatus.ACCEPTED));
							}
							device.setLastUpdate(LocalDateTime.now());
							return deviceRepository.save(device).flatMap(device1 -> Mono.just(new ResponseEntity<>("OK", HttpStatus.OK)))
									.onErrorResume(throwable -> {
										logger.warn("Error when trying update device lastUpdatedDate : ", throwable);
										return Mono.just(new ResponseEntity<>("Internal server error !", HttpStatus.INTERNAL_SERVER_ERROR));
									});
						})
						.onErrorResume(throwable -> {
							logger.warn("Error when trying to get device from device ID : ", throwable);
							return Mono.just(new ResponseEntity<>("Internal server error !", HttpStatus.INTERNAL_SERVER_ERROR));
						});
					});
		} catch (IOException e) {
			logger.warn("Error when parsing JSON for downlink to TTN !", e);
			return Mono.just(new ResponseEntity<>("Error when parsing JSON : "+e.getMessage(), HttpStatus.BAD_REQUEST));
		}
	}
}
