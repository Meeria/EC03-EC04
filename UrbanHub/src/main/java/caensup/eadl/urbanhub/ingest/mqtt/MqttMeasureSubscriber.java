package caensup.eadl.urbanhub.ingest.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestService;

import jakarta.annotation.PreDestroy;

/**
 * Souscrit au broker MQTT et achemine les messages vers {@link MeasureIngestService}.
 *
 * <p>Activé uniquement si {@code mqtt.enabled=true} (variable d'env {@code MQTT_ENABLED}, défaut : {@code true}).
 * La connexion s'établit sur {@link org.springframework.boot.context.event.ApplicationReadyEvent}
 * et se ferme proprement au shutdown via {@link jakarta.annotation.PreDestroy}.
 */
@Component
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttMeasureSubscriber {

    private static final Logger log = LoggerFactory.getLogger(MqttMeasureSubscriber.class);

    private final MeasureIngestService measureIngestService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    private IMqttClient mqttClient;

    public MqttMeasureSubscriber(MeasureIngestService measureIngestService, ObjectMapper objectMapper) {
        this.measureIngestService = measureIngestService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        log.info("Connecting to MQTT broker at {} ...", brokerUrl);
        mqttClient.connect(options);
        log.info("Connected – subscribing to {}", topic);

        mqttClient.subscribe(topic, 1, (t, msg) -> {
            try {
                IngestMeasureJson json = objectMapper.readValue(msg.getPayload(), IngestMeasureJson.class);
                measureIngestService.ingestMeasure(json);
            } catch (Exception e) {
                log.error("Failed to process MQTT message on topic {}: {}", t, e.getMessage());
            }
        });
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("Disconnected from MQTT broker");
            } catch (MqttException e) {
                log.error("Error disconnecting from MQTT broker: {}", e.getMessage());
            }
        }
    }
}
