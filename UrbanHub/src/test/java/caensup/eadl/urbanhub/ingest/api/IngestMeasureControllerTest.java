package caensup.eadl.urbanhub.ingest.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;
import caensup.eadl.urbanhub.ingest.exception.SensorNotFoundException;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class IngestMeasureControllerTest {

        @Mock
        private MeasureIngestService measureIngestService;

        private MockMvc mockMvc;

        private static final String VALID_JSON = """
                        {
                          "sensor_id": "CAP-001",
                          "type": "air",
                          "timestamp": "1744538100000",
                          "location": "49.18;-0.37",
                          "value": 25.0,
                          "unit": "\\u03bcg/m3"
                        }
                        """;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(new IngestMeasureController(measureIngestService))
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        @DisplayName("POST /ingest/measures avec corps valide retourne 200")
        void ingestValidMeasureShouldReturn200() throws Exception {
                doNothing().when(measureIngestService).ingestMeasure(any());

                mockMvc.perform(post("/ingest/measures")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_JSON))
                                .andExpect(status().isOk());

                verify(measureIngestService).ingestMeasure(any());
        }

        @Test
        @DisplayName("POST /ingest/measures avec champs manquants retourne 400")
        void ingestMissingRequiredFieldsShouldReturn400() throws Exception {
                mockMvc.perform(post("/ingest/measures")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /ingest/measures avec JSON malformé retourne 400")
        void ingestMalformedJsonShouldReturn400() throws Exception {
                mockMvc.perform(post("/ingest/measures")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("not-valid-json{"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /ingest/measures avec mesure invalide retourne 422")
        void ingestInvalidMeasureShouldReturn422() throws Exception {
                doThrow(new InvalidMeasureException("valeur invalide"))
                                .when(measureIngestService).ingestMeasure(any());

                mockMvc.perform(post("/ingest/measures")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_JSON))
                                .andExpect(status().is(422));
        }

        @Test
        @DisplayName("POST /ingest/measures avec capteur inconnu retourne 404")
        void ingestUnknownSensorShouldReturn404() throws Exception {
                doThrow(new SensorNotFoundException("CAP-999"))
                                .when(measureIngestService).ingestMeasure(any());

                mockMvc.perform(post("/ingest/measures")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_JSON))
                                .andExpect(status().isNotFound());
        }
}
