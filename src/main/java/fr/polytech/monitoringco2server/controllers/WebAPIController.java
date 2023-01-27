package fr.polytech.monitoringco2server.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.influxdb.client.reactive.InfluxDBClientReactive;
import com.influxdb.client.reactive.QueryReactiveApi;
import com.influxdb.query.FluxRecord;
import fr.polytech.monitoringco2server.database.documents.Device;
import fr.polytech.monitoringco2server.database.repositories.DeviceRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/webapi")
public class WebAPIController {

	private static final Logger logger = LoggerFactory.getLogger("WebAPIController.java");

	final DeviceRepository deviceRepository;
	final InfluxDBClientReactive influxDBClientReactive;

	public WebAPIController(DeviceRepository deviceRepository, InfluxDBClientReactive influxDBClientReactive) {
		this.deviceRepository = deviceRepository;
		this.influxDBClientReactive = influxDBClientReactive;
	}

	@GetMapping("/devices")
	public Mono<ResponseEntity<String>> getDevices(Authentication authentication){
		Flux<Device> deviceFlux;
		if(authentication != null){
			deviceFlux = deviceRepository.findAll();
		}else{
			deviceFlux = deviceRepository.findAllByPublicDeviceIsTrue();
		}

		return deviceFlux.collectList().flatMap(devices -> {
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode rootNode = mapper.createArrayNode();
			for (Device device: devices) {
				ObjectNode deviceNode = mapper.createObjectNode();
				deviceNode.put("id", device.getId().toString());
				deviceNode.put("displayName", device.getDisplayName());
				deviceNode.put("room", device.getRoom());
				if(device.getLastUpdate() != null){
					deviceNode.put("lastUpdated", device.getLastUpdate().toString());
				}
				else{
					deviceNode.put("lastUpdated", "null");
				}
				if(device.getLastCo2Value() != null){
					deviceNode.put("lastCo2Value", device.getLastCo2Value());
				}
				else{
					deviceNode.put("lastCo2Value", "null");
				}
				rootNode.add(deviceNode);
			}
			try {
				String jsonString = mapper.writer().writeValueAsString(rootNode);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				return Mono.just(new ResponseEntity<>(jsonString, headers, HttpStatus.OK));
			} catch (JsonProcessingException e) {
				logger.warn("Internal error generating JSON", e);
				return Mono.just(new ResponseEntity<>("Internal JSON error", HttpStatus.INTERNAL_SERVER_ERROR));
			}
		})
		.onErrorResume(throwable -> {
			logger.warn("Internal error getting devices", throwable);
			return Mono.just(new ResponseEntity<>("Internal server error !", HttpStatus.INTERNAL_SERVER_ERROR));
		});
	}

	@GetMapping("/device/{id}/lastValues")
	public Mono<ResponseEntity<String>> getDeviceLastValues(@PathVariable String id, Authentication authentication){
		ObjectId deviceDbId;
		try{
			deviceDbId = new ObjectId(id);
		}
		catch (IllegalArgumentException e){
			return Mono.just(new ResponseEntity<>("Bad ID !", HttpStatus.BAD_REQUEST));
		}

		return deviceRepository.findById(deviceDbId).flatMap(device -> {
			if(!device.isPublicDevice() && authentication == null){
				return Mono.just(new ResponseEntity<>("This device is private !", HttpStatus.UNAUTHORIZED));
			}
			QueryReactiveApi queryApi = influxDBClientReactive.getQueryReactiveApi();
			String flux = "from(bucket: \"Monitoring CO2 Data\") |> range(start:0) |> filter(fn: (r) => r[\"_measurement\"] == \"co2\" or r[\"_measurement\"] == \"temperature\" or r[\"_measurement\"] == \"humidite\" or r[\"_measurement\"] == \"batterie\") |> filter(fn: (r) => r[\"_field\"] == \"value\") |> filter(fn: (r) => r[\"deviceId\"] == \""+device.getDeviceId()+"\") |> last()";

			return Flux.from(queryApi.query(flux)).collectList().flatMap(fluxRecords -> {
				if(fluxRecords.size() == 0){
					return Mono.just(new ResponseEntity<>("No data", HttpStatus.NOT_FOUND));
				}

				ObjectMapper mapper = new ObjectMapper();
				ObjectNode rootNode = mapper.createObjectNode();
				for (FluxRecord fluxRecord: fluxRecords) {
					if(fluxRecord.getValue() instanceof Double){
						rootNode.put(fluxRecord.getMeasurement(), (Double) fluxRecord.getValue());
					}
					else if(fluxRecord.getValue() instanceof Integer){
						rootNode.put(fluxRecord.getMeasurement(), (Integer) fluxRecord.getValue());
					}
					else if(fluxRecord.getValue() instanceof Long){
						rootNode.put(fluxRecord.getMeasurement(), (Long) fluxRecord.getValue());
					}
					else if(fluxRecord.getValue() != null){
						rootNode.put(fluxRecord.getMeasurement(), fluxRecord.getValue().toString());
					}
				}
				rootNode.put("timestamp", Objects.requireNonNull(fluxRecords.get(0).getTime()).toString());
				String jsonStr;
				try{
					jsonStr = mapper.writer().writeValueAsString(rootNode);
				} catch (JsonProcessingException e) {
					logger.warn("Error generating device values JSON !", e);
					return Mono.just(new ResponseEntity<>("Internal JSON error !", HttpStatus.INTERNAL_SERVER_ERROR));
				}

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				return Mono.just(new ResponseEntity<>(jsonStr, headers, HttpStatus.OK));
			});

		}).switchIfEmpty(Mono.just(new ResponseEntity<>("Unknown device !", HttpStatus.BAD_REQUEST)));
	}
}
