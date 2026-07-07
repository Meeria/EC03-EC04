package caensup.eadl.urbanhub.ingest.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.mqtt.MqttMeasureSubscriber;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestService;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IngestMeasureMqttTest {

        @Mock
        private MeasureIngestService measureIngestService;

        @Mock
        private ObjectMapper objectMapper;

        private MqttMeasureSubscriber subscriber;

        private static final String BROKER_URL = "tcp://localhost:1883";
        private static final String CLIENT_ID = "test-client";
        private static final String TOPIC = "test/topic";

        private static final String VALID_PAYLOAD = """
                        {"sensor_id":"CAP-001","type":"air","timestamp":"1744538100000","location":"49.18;-0.37","value":25.0,"unit":"µg/m3"}
                        """;

        @BeforeEach
        void setUp() {
                subscriber = new MqttMeasureSubscriber(measureIngestService, objectMapper);
                ReflectionTestUtils.setField(subscriber, "brokerUrl", BROKER_URL);
                ReflectionTestUtils.setField(subscriber, "clientId", CLIENT_ID);
                ReflectionTestUtils.setField(subscriber, "topic", TOPIC);
        }

        @Test
        @DisplayName("connect() doit se connecter au broker et s'abonner au topic")
        void connectShouldConnectAndSubscribe() throws Exception {
                try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class)) {
                        subscriber.connect();

                        MqttClient mockClient = mocked.constructed().get(0);
                        verify(mockClient).connect(any(MqttConnectOptions.class));
                        verify(mockClient).subscribe(eq(TOPIC), eq(1), any(IMqttMessageListener.class));
                }
        }

        @Test
        @DisplayName("Le callback MQTT doit désérialiser et ingérer un message valide")
        void callbackShouldDeserializeAndIngest() throws Exception {
                IngestMeasureJson expectedJson = new IngestMeasureJson(
                                "CAP-001", "air", "1744538100000", "49.18;-0.37", 25.0, "µg/m3");

                try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class)) {
                        subscriber.connect();

                        MqttClient mockClient = mocked.constructed().get(0);
                        ArgumentCaptor<IMqttMessageListener> listenerCaptor =
                                        ArgumentCaptor.forClass(IMqttMessageListener.class);
                        verify(mockClient).subscribe(eq(TOPIC), eq(1), listenerCaptor.capture());

                        when(objectMapper.readValue(any(byte[].class), eq(IngestMeasureJson.class)))
                                        .thenReturn(expectedJson);

                        MqttMessage message = new MqttMessage(VALID_PAYLOAD.getBytes());
                        listenerCaptor.getValue().messageArrived(TOPIC, message);

                        verify(measureIngestService).ingestMeasure(expectedJson);
                }
        }

        @Test
        @DisplayName("Le callback MQTT doit gérer les erreurs sans propager l'exception")
        void callbackShouldHandleDeserializationError() throws Exception {
                try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class)) {
                        subscriber.connect();

                        MqttClient mockClient = mocked.constructed().get(0);
                        ArgumentCaptor<IMqttMessageListener> listenerCaptor =
                                        ArgumentCaptor.forClass(IMqttMessageListener.class);
                        verify(mockClient).subscribe(eq(TOPIC), eq(1), listenerCaptor.capture());

                        when(objectMapper.readValue(any(byte[].class), eq(IngestMeasureJson.class)))
                                        .thenThrow(new RuntimeException("JSON invalide"));

                        MqttMessage message = new MqttMessage("bad-json".getBytes());
                        listenerCaptor.getValue().messageArrived(TOPIC, message);

                        verify(measureIngestService, never()).ingestMeasure(any());
                }
        }

        @Test
        @DisplayName("disconnect() doit déconnecter et fermer le client MQTT")
        void disconnectShouldDisconnectAndClose() throws Exception {
                IMqttClient mockClient = mock(IMqttClient.class);
                when(mockClient.isConnected()).thenReturn(true);
                ReflectionTestUtils.setField(subscriber, "mqttClient", mockClient);

                subscriber.disconnect();

                verify(mockClient).disconnect();
                verify(mockClient).close();
        }

        @Test
        @DisplayName("disconnect() ne doit rien faire si le client est null")
        void disconnectShouldDoNothingWhenClientIsNull() {
                assertDoesNotThrow(() -> subscriber.disconnect());
        }

        @Test
        @DisplayName("disconnect() doit gérer les exceptions MQTT gracieusement")
        void disconnectShouldHandleMqttException() throws Exception {
                IMqttClient mockClient = mock(IMqttClient.class);
                when(mockClient.isConnected()).thenReturn(true);
                doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                                .when(mockClient).disconnect();
                ReflectionTestUtils.setField(subscriber, "mqttClient", mockClient);

                assertDoesNotThrow(() -> subscriber.disconnect());
        }
}
