package caensup.eadl.urbanhub.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.dto.SensorTypeDto;
import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.ingest.exception.SensorTypeNotFoundException;
import caensup.eadl.urbanhub.service.SensorTypeService;
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
class SensorTypeControllerTest {

    @Mock
    private SensorTypeService sensorTypeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SensorTypeController(sensorTypeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/sensor-types retourne tous les types de capteur")
    void getAllShouldReturnSensorTypeList() throws Exception {
        when(sensorTypeService.getAll()).thenReturn(List.of(buildDto("AIR"), buildDto("NOISE")));

        mockMvc.perform(get("/api/sensor-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sensorTypeId").value("AIR"))
                .andExpect(jsonPath("$[1].sensorTypeId").value("NOISE"));

        verify(sensorTypeService).getAll();
    }

    @Test
    @DisplayName("GET /api/sensor-types/count retourne le nombre de types")
    void getCountShouldReturnCount() throws Exception {
        when(sensorTypeService.getCount()).thenReturn(4L);

        mockMvc.perform(get("/api/sensor-types/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));

        verify(sensorTypeService).getCount();
    }

    @Test
    @DisplayName("GET /api/sensor-types/by-id retourne le type correspondant")
    void getByIdShouldReturnSensorType() throws Exception {
        when(sensorTypeService.getById("AIR")).thenReturn(buildDto("AIR"));

        mockMvc.perform(get("/api/sensor-types/by-id").param("sensor_type_id", "AIR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorTypeId").value("AIR"));

        verify(sensorTypeService).getById("AIR");
    }

    @Test
    @DisplayName("GET /api/sensor-types/by-id retourne 404 si le type est introuvable")
    void getByIdShouldReturn404WhenTypeNotFound() throws Exception {
        when(sensorTypeService.getById("UNKNOWN")).thenThrow(new SensorTypeNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/sensor-types/by-id").param("sensor_type_id", "UNKNOWN"))
                .andExpect(status().isNotFound());

        verify(sensorTypeService).getById("UNKNOWN");
    }

    private SensorTypeDto buildDto(String typeId) {
        return new SensorTypeDto(UUID.randomUUID(), typeId);
    }
}
