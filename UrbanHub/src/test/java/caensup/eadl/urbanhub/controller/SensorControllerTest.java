package caensup.eadl.urbanhub.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.dto.SensorDto;
import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.service.SensorService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SensorControllerTest {

    @Mock
    private SensorService sensorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SensorController(sensorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/sensors/status/count retourne le nombre de capteurs pour le filtre alive")
    void getByStatusCountShouldReturnCount() throws Exception {
        when(sensorService.getByStatusCount(true)).thenReturn(3L);

        mockMvc.perform(get("/api/sensors/status/count").param("alive", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

        verify(sensorService).getByStatusCount(true);
    }

    @Test
    @DisplayName("GET /api/sensors/status retourne la liste des capteurs")
    void getByStatusShouldReturnSensorList() throws Exception {
        UUID u = UUID.randomUUID();
        when(sensorService.getByStatus(true))
                .thenReturn(List.of(new SensorDto(u, "S1", "AIR", 0.0, 0.0, true, null)));

        mockMvc.perform(get("/api/sensors/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sensorId").value("S1"))
                .andExpect(jsonPath("$[0].sensorTypeId").value("AIR"));

        verify(sensorService).getByStatus(true);
    }
}
