package caensup.eadl.urbanhub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.ingest.api.IngestMeasureController;
import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class IngestControllerTest {

    @Mock
    private MeasureIngestServiceImpl measureIngestServiceImpl;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IngestMeasureController(measureIngestServiceImpl))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validMeasureShouldReturn200() throws Exception {

        mockMvc.perform(post("/ingest/measures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensor_id\": \"CAP-001\", \"type\": \"WEATHER\", \"timestamp\": \"1718236800000\", \"location\": \"49.1828; -0.3706\", \"value\": 18.5, \"unit\": \"°C\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
        "WEATHER, 18.5, km/h",
        "AIR, 42.5, °C",
        "NOISE, 65.3, μg/m3",
        "TRAFFIC, 120.0, dB"
    })
    void invalidMeasureShouldReturn422(String type, Double value, String unit) throws Exception {
        doThrow(new InvalidMeasureException("Invalid unit for type " + type))
                .when(measureIngestServiceImpl).ingestMeasure(any());

        mockMvc.perform(post("/ingest/measures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensor_id\": \"CAP-001\", \"type\": \"" + type + "\", \"timestamp\": \"1718236800000\", \"location\": \"49.1828; -0.3706\", \"value\": " + value + ", \"unit\": \"" + unit + "\"}"))
                .andExpect(status().is(422));
    }

    @Test
    void invalidMethodArgumentNotValidExceptionShouldReturn400() throws Exception {

        mockMvc.perform(post("/ingest/measures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensor_id\": \"CAP-001\", \"type\": \"WEATHER\", \"timestamp\": \"1718236800000\", \"location\": \"49.1828; -0.3706\", \"value\": 18.5}"))
                .andExpect(status().is(400));
    }

    @Test
    void invalidJsonShouldReturn400() throws Exception {
        mockMvc.perform(post("/ingest/measures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensor_id\": \"CAP-001\", \"type\": \"WEATHER\", \"timestamp\""))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.detail").value("Unreadable or malformed JSON body"));
    }

}

