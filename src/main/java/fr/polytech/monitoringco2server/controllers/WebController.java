package fr.polytech.monitoringco2server.controllers;

import fr.polytech.monitoringco2server.database.documents.Device;
import fr.polytech.monitoringco2server.database.repositories.DeviceRepository;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class WebController {

	final DeviceRepository deviceRepository;

	public WebController(DeviceRepository deviceRepository) {
		this.deviceRepository = deviceRepository;
	}

	@GetMapping("/")
	public Mono<String> homePage() {
		return Mono.just("index.html");
	}

	@GetMapping("/devices")
	public Mono<String> devicesPage(Authentication authentication, Model map) {
		Flux<Device> deviceFlux;

		if(authentication != null){
			deviceFlux = deviceRepository.findAll();
		}
		else{
			deviceFlux = deviceRepository.findAllByPublicDeviceIsTrue();
		}

		return deviceFlux.collectList().flatMap(devices -> {
			map.addAttribute("devices", devices);
			return Mono.just("list_devices.html");
		})
		.onErrorResume(throwable -> {
			map.addAttribute("error", throwable.toString());
			return Mono.just("list_devices.html");
		});
	}

	@GetMapping("/devices/add")
	public String addDevicePage(Model map) {
		if(!map.containsAttribute("device")) {
			map.addAttribute("device", new Device());
		}
		if(!map.containsAttribute("rooms")){
			map.addAttribute("rooms",
					List.of("301", "302", "303",
							"304", "305", "306", "308",
							"310", "312", "314", "316",
							"317", "318", "319", "320",
							"321", "322", "323", "324",
							"325", "326", "327", "328",
							"330", "331", "322", "333",
							"334", "335"));
		}

		return "add_device.html";
	}

	@PostMapping("/devices/add")
	public Mono<String> addDeviceSubmit(@Valid Device device, BindingResult result, Model map, Authentication authentication) {
		map.addAttribute("device", device);
		if(result.hasErrors()){
			return Mono.just("add_device.html");
		}

		return deviceRepository.insert(device)
				.flatMap(device1 -> devicesPage(authentication, map))
				.onErrorResume(throwable -> {
					map.addAttribute("databaseError", throwable.toString());
					return Mono.just("add_device.html");
				});
	}

	@GetMapping("/devices/{id}/data")
	public Mono<String> deviceData(@PathVariable String id, Authentication authentication, Model map){
		ObjectId deviceDbId;
		try{
			deviceDbId = new ObjectId(id);
		}
		catch (IllegalArgumentException e){
			map.addAttribute("error", "Bad ID !");
			return Mono.just("device_data.html");
		}

		return deviceRepository.findById(deviceDbId).flatMap(device -> {
			if(!device.isPublicDevice() && authentication == null){
				map.addAttribute("This device is private !");
				return Mono.just("device_data.html");
			}
			map.addAttribute("device", device);
			return Mono.just("device_data.html");
		}).switchIfEmpty(Mono.fromCallable(() -> {
			map.addAttribute("error", "Unknown device !");
			return "device_data.html";
		}));
	}
}
