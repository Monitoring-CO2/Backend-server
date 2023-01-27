package fr.polytech.monitoringco2server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.polytech.monitoringco2server.LoRa.Downlink;
import fr.polytech.monitoringco2server.LoRa.PayloadDecoder;
import fr.polytech.monitoringco2server.database.repositories.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class APIController {

	private static final Logger logger = LoggerFactory.getLogger("APIController.java");

	final Downlink downlink;
	final PayloadDecoder payloadDecoder;
	final DeviceRepository deviceRepository;

	@Value("${ttn.downlinkApiKey}")
	String TTN_DOWNLINK_API_KEY;

	public APIController(Downlink downlink, PayloadDecoder payloadDecoder, DeviceRepository deviceRepository) {
		this.downlink = downlink;
		this.payloadDecoder = payloadDecoder;
		this.deviceRepository = deviceRepository;
	}

	@PostMapping("/devices/join")
	public Mono<ResponseEntity<String>> deviceJoin(final ServerWebExchange swe){
		if(!swe.getRequest().getHeaders().containsKey("X-API-Key")){
			return Mono.just(new ResponseEntity<>("You need to provide the downlink API key !", HttpStatus.UNAUTHORIZED));
		}
		else if(!swe.getRequest().getHeaders().getFirst("X-API-Key").equals(TTN_DOWNLINK_API_KEY)){
			return Mono.just(new ResponseEntity<>("Wrong downlink API key !", HttpStatus.UNAUTHORIZED));
		}

		return DataBufferUtils.join(swe.getRequest().getBody()).flatMap(dataBuffer -> {
			byte[] bytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(bytes);
			DataBufferUtils.release(dataBuffer);

			return downlink.downlinkTimeSync(bytes);

		}).onErrorReturn(new ResponseEntity<>("Internal server error !", HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@PostMapping("/devices/uplink")
	public Mono<ResponseEntity<String>> deviceUplink(final ServerWebExchange swe){
		if(!swe.getRequest().getHeaders().containsKey("X-API-Key")){
			return Mono.just(new ResponseEntity<>("You need to provide the downlink API key !", HttpStatus.UNAUTHORIZED));
		}
		else if(!swe.getRequest().getHeaders().getFirst("X-API-Key").equals(TTN_DOWNLINK_API_KEY)){
			return Mono.just(new ResponseEntity<>("Wrong downlink API key !", HttpStatus.UNAUTHORIZED));
		}

		return DataBufferUtils.join(swe.getRequest().getBody()).flatMap(dataBuffer -> {
			byte[] bytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(bytes);
			DataBufferUtils.release(dataBuffer);

			ObjectMapper objectMapper = new ObjectMapper();
			try {
				JsonNode globalOjb = objectMapper.readTree(bytes);
				String applicationId = globalOjb.get("end_device_ids").get("application_ids").get("application_id").asText();
				String deviceId = globalOjb.get("end_device_ids").get("device_id").asText();
				String encodedPayload = globalOjb.get("uplink_message").get("frm_payload").asText();
				byte[] decodedPayload = Base64.getDecoder().decode(encodedPayload);
				int type = globalOjb.get("uplink_message").get("f_port").asInt();

				if(type == 3){

					if(decodedPayload[0] == 0){
						return Mono.just(new ResponseEntity<>("No messages in data payload", HttpStatus.ACCEPTED));
					}

					return payloadDecoder.processDataPayload(decodedPayload, deviceId)
							.flatMap(success -> deviceRepository.findByDeviceIdIs(deviceId).flatMap(device -> {
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
									})
							.onErrorResume(throwable -> throwable instanceof Exception,
									throwable -> {
								logger.warn("Error when saving data in InfluxDB for "+deviceId, throwable);
								return Mono.just(new ResponseEntity<>("Error when saving data in InfluxDB : "+throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
							}));
				}
				else if(type == 255){
					return Mono.just(new ResponseEntity<>("Ignored", HttpStatus.ACCEPTED));
				}
				else{
					return Mono.just(new ResponseEntity<>("Unknown first byte (type) : 0x"+String.format("%02X", type), HttpStatus.BAD_REQUEST));
				}
			} catch (IOException e) {
				return Mono.just(new ResponseEntity<>("Unable to read JSON : " + e.getMessage(), HttpStatus.BAD_REQUEST));
			}
		}).onErrorReturn(new ResponseEntity<>("Internal server error !", HttpStatus.INTERNAL_SERVER_ERROR));
	}
}
