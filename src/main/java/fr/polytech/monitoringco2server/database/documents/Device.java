package fr.polytech.monitoringco2server.database.documents;

import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;
import reactor.util.annotation.NonNull;

import java.time.LocalDateTime;

@Document(collection = "devices")
public class Device {

	@Id
	@NonNull
	private ObjectId id;

	@NotBlank(message = "Merci de saisir l''ID de l''appareil")
	private String deviceId;

	@NotBlank(message = "Merci de saisir le DevEUI de l''appareil")
	private String devEUI;

	@NotBlank(message = "Merci de saisir un nom pour l''appareil")
	private String displayName;

	@NotBlank(message = "Merci de sélectionner la salle dans laquelle se trouve l''appareil")
	private String room;

	private boolean publicDevice = true;
	@NonNull
	private LocalDateTime createDate = LocalDateTime.now();
	private LocalDateTime lastUpdate = null;

	public Device(){};

	public Device(@NotBlank String displayName, @NotBlank String deviceId, @NotBlank String devEUI, @NotBlank String room, boolean publicDevice){
		this.displayName = displayName;
		this.deviceId = deviceId;
		this.devEUI = devEUI;
		this.room = room;
		this.publicDevice = publicDevice;
	}

	@PersistenceCreator
	public Device(@NonNull ObjectId id, String deviceId, String devEUI, String displayName, String room, boolean publicDevice, @NonNull LocalDateTime createDate, LocalDateTime lastUpdate) {
		this.id = id;
		this.deviceId = deviceId;
		this.devEUI = devEUI;
		this.displayName = displayName;
		this.room = room;
		this.publicDevice = publicDevice;
		this.createDate = createDate;
		this.lastUpdate = lastUpdate;
	}

	public ObjectId getId() {
		return id;
	}

	public void setDeviceId(String deviceId){
		this.deviceId = deviceId;
	}
	public String getDeviceId() {
		return deviceId;
	}

	public void setDevEUI(String devEUI){
		this.devEUI = devEUI;
	}
	public String getDevEUI() {
		return devEUI;
	}

	public void setDisplayName(String displayName){
		this.displayName = displayName;
	}
	public String getDisplayName() {
		return displayName;
	}

	public void setRoom(String room){
		this.room = room;
	}
	public String getRoom() {
		return room;
	}

	public void setPublicDevice(boolean publicDevice){
		this.publicDevice = publicDevice;
	}
	public boolean isPublicDevice() {
		return publicDevice;
	}

	public LocalDateTime getCreateDate() {
		return createDate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate){
		this.lastUpdate = lastUpdate;
	}
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}
}
