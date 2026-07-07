package caensup.eadl.urbanhub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.dto.ZoneDto;
import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.ingest.exception.ZoneNotFoundException;
import caensup.eadl.urbanhub.service.ZoneService;
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
class ZoneControllerTest {

    @Mock
    private ZoneService zoneService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ZoneController(zoneService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/zones retourne toutes les zones")
    void getAllShouldReturnZoneList() throws Exception {
        when(zoneService.getAll()).thenReturn(List.of(buildDto("CENTRE"), buildDto("NORD")));

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneId").value("CENTRE"))
                .andExpect(jsonPath("$[1].zoneId").value("NORD"));

        verify(zoneService).getAll();
    }

    @Test
    @DisplayName("GET /api/zones/count retourne le nombre de zones")
    void getCountShouldReturnCount() throws Exception {
        when(zoneService.getCount()).thenReturn(5L);

        mockMvc.perform(get("/api/zones/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(zoneService).getCount();
    }

    @Test
    @DisplayName("GET /api/zones/by-id retourne la zone correspondante")
    void getByIdShouldReturnZone() throws Exception {
        when(zoneService.getById("CENTRE")).thenReturn(buildDto("CENTRE"));

        mockMvc.perform(get("/api/zones/by-id").param("zone_id", "CENTRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value("CENTRE"));

        verify(zoneService).getById("CENTRE");
    }

    @Test
    @DisplayName("GET /api/zones/by-id retourne 404 si la zone est introuvable")
    void getByIdShouldReturn404WhenZoneNotFound() throws Exception {
        when(zoneService.getById("UNKNOWN")).thenThrow(new ZoneNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/zones/by-id").param("zone_id", "UNKNOWN"))
                .andExpect(status().isNotFound());

        verify(zoneService).getById("UNKNOWN");
    }

    @Test
    @DisplayName("POST /api/zones crée une zone et la retourne")
    void createShouldReturnCreatedZone() throws Exception {
        when(zoneService.create(any())).thenReturn(buildDto("ZONE_NORD"));

        mockMvc.perform(post("/api/zones")
                .contentType("application/json")
                .content("""
                    { "zoneId": "ZONE_NORD", "sensorIds": ["SENSOR_01", "SENSOR_02"] }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value("ZONE_NORD"));

        verify(zoneService).create(any());
    }

    @Test
    @DisplayName("PUT /api/zones/{zoneId} met à jour la zone et la retourne")
    void updateShouldReturnUpdatedZone() throws Exception {
        when(zoneService.update(eq("CENTRE"), any())).thenReturn(buildDto("CENTRE"));

        mockMvc.perform(put("/api/zones/CENTRE")
                .contentType("application/json")
                .content("""
                    { "zoneId": "CENTRE", "sensorIds": ["SENSOR_03"] }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value("CENTRE"));

        verify(zoneService).update(eq("CENTRE"), any());
    }

    @Test
    @DisplayName("PUT /api/zones/{zoneId} retourne 404 si la zone n'existe pas")
    void updateShouldReturn404WhenZoneNotFound() throws Exception {
        when(zoneService.update(eq("UNKNOWN"), any())).thenThrow(new ZoneNotFoundException("UNKNOWN"));

        mockMvc.perform(put("/api/zones/UNKNOWN")
                .contentType("application/json")
                .content("""
                    { "zoneId": "UNKNOWN", "sensorIds": [] }
                    """))
                .andExpect(status().isNotFound());

        verify(zoneService).update(eq("UNKNOWN"), any());
    }

    @Test
    @DisplayName("DELETE /api/zones/{zoneId} supprime la zone et retourne 204")
    void deleteShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/zones/CENTRE"))
                .andExpect(status().isOk());

        verify(zoneService).delete("CENTRE");
    }

    @Test
    @DisplayName("DELETE /api/zones/{zoneId} retourne 404 si la zone n'existe pas")
    void deleteShouldReturn404WhenZoneNotFound() throws Exception {
        doThrow(new ZoneNotFoundException("UNKNOWN")).when(zoneService).delete("UNKNOWN");

        mockMvc.perform(delete("/api/zones/UNKNOWN"))
                .andExpect(status().isNotFound());

        verify(zoneService).delete("UNKNOWN");
    }

    private ZoneDto buildDto(String zoneId) {
        return new ZoneDto(UUID.randomUUID(), zoneId, List.of());
    }
}