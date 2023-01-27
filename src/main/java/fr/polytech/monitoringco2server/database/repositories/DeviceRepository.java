package fr.polytech.monitoringco2server.database.repositories;

import fr.polytech.monitoringco2server.database.documents.Device;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DeviceRepository extends ReactiveMongoRepository<Device, ObjectId> {

	@NotNull
	@Override
	Flux<Device> findAll();

	@NotNull
	Flux<Device> findAllByPublicDeviceIsTrue();

	@NotNull
	Mono<Device> findById(@NotNull ObjectId id);

	@NotNull
	Mono<Device> findByDeviceIdIs(String deviceId);
}
