package fr.polytech.monitoringco2server.LoRa;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.reactive.InfluxDBClientReactive;
import com.influxdb.client.reactive.WriteReactiveApi;
import com.influxdb.client.write.Point;
import fr.polytech.monitoringco2server.database.repositories.DeviceRepository;
import io.reactivex.rxjava3.core.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PayloadDecoder {

	final InfluxDBClientReactive influxDBClientReactive;

	private static final Logger logger = LoggerFactory.getLogger("PayloadDecoder.java");

	public PayloadDecoder(InfluxDBClientReactive influxDBClientReactive) {
		this.influxDBClientReactive = influxDBClientReactive;
	}

	public Publisher<WriteReactiveApi.Success> processDataPayload(byte[] payload, String deviceId){
		int messageCount = payload[0];
		float battery = 2.5F + ((float)payload[1])/10;

		logger.debug("Received "+messageCount+" message(s) from "+deviceId);

		WriteReactiveApi writeApi = influxDBClientReactive.getWriteReactiveApi();

		List<Point> receivedData = new ArrayList<>();

		Point point = new Point("batterie");
		point.time(Instant.now().toEpochMilli(), WritePrecision.MS);
		point.addTag("deviceId", deviceId);
		point.addField("value", battery);
		receivedData.add(point);

		for(int n = 0 ; n < messageCount && (8*n+9) < payload.length ; n ++){
			long timestamp = ByteBuffer.wrap(payload, 8*n+2, 8).getLong() >> 32;

			float temperature = (((payload[8*n+6] << 2) | ((payload[8*n+7] & 0xC0) >> 6)) / 10F) - 25;
			point = new Point("temperature");
			point.time(timestamp, WritePrecision.S);
			point.addTag("deviceId", deviceId);
			point.addField("value", temperature);
			receivedData.add(point);

			int humidite = ((payload[8*n+7] & 0x3F) << 1) | ((payload[8*n+8] & 0x80) >> 7);
			point = new Point("humidite");
			point.time(timestamp, WritePrecision.S);
			point.addTag("deviceId", deviceId);
			point.addField("value", humidite);
			receivedData.add(point);


			int co2 = (((payload[8*n+8] & 0x7F) << 4) | ((payload[8*n+9] & 0xF0) >> 4))*10;
			point = new Point("co2");
			point.time(timestamp, WritePrecision.S);
			point.addTag("deviceId", deviceId);
			point.addField("value", co2);
			receivedData.add(point);

			int mouvement = payload[8*n+9] & 0x0F;
			point = new Point("mouvement");
			point.time(timestamp, WritePrecision.S);
			point.addTag("deviceId", deviceId);
			point.addField("value", mouvement);
			receivedData.add(point);
		}

		Flowable<Point> pointFlowable = Flowable.fromIterable(receivedData);

		return writeApi.writePoints(WritePrecision.S, pointFlowable);
	}
}
