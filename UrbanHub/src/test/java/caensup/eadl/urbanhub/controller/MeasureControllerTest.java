package caensup.eadl.urbanhub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import caensup.eadl.urbanhub.dto.MeasureDto;
import caensup.eadl.urbanhub.ingest.exception.GlobalExceptionHandler;
import caensup.eadl.urbanhub.service.MeasureQueryService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MeasureControllerTest {

        @Mock
        private MeasureQueryService measureQueryService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(new MeasureController(measureQueryService))
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        @DisplayName("GET /api/measures retourne la liste de toutes les mesures")
        void getMeasuresShouldReturnMeasureList() throws Exception {
                MeasureDto dto = buildDto();
                when(measureQueryService.getMeasures(null)).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/measures"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].timestamp").exists())
                                .andExpect(jsonPath("$[0].sensorId").value("CAP-001"));

                verify(measureQueryService).getMeasures(null);
        }

        @Test
        @DisplayName("GET /api/measures?sensor_id= filtre par identifiant capteur")
        void getMeasuresShouldForwardSensorIdFilter() throws Exception {
                MeasureDto dto = buildDto();
                when(measureQueryService.getMeasures("CAP-001")).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/measures").param("sensor_id", "CAP-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].sensorId").value("CAP-001"));

                verify(measureQueryService).getMeasures("CAP-001");
        }

        @Test
        @DisplayName("GET /api/measures/count retourne le nombre total de mesures")
        void getCountShouldReturnTotalCount() throws Exception {
                when(measureQueryService.getCount()).thenReturn(42L);

                mockMvc.perform(get("/api/measures/count"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("42"));

                verify(measureQueryService).getCount();
        }

        @Test
        @DisplayName("GET /api/measures/by-day retourne les mesures d'une journée")
        void getMeasuresByDayShouldReturnMeasures() throws Exception {
                MeasureDto dto = buildDto();
                when(measureQueryService.getMeasuresByDay("2026-04-13")).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/measures/by-day").param("date", "2026-04-13"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].sensorId").value("CAP-001"));

                verify(measureQueryService).getMeasuresByDay("2026-04-13");
        }

        @Test
        @DisplayName("GET /api/measures/by-day retourne 400 pour un format de date invalide")
        void getMeasuresByDayShouldReturn400ForInvalidDate() throws Exception {
                when(measureQueryService.getMeasuresByDay("invalid"))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Invalid date format, expected yyyy-MM-dd"));

                mockMvc.perform(get("/api/measures/by-day").param("date", "invalid"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/measures/by-date-range retourne les mesures entre deux dates")
        void getMeasuresByDateRangeShouldReturnMeasures() throws Exception {
                MeasureDto dto = buildDto();
                when(measureQueryService.getMeasuresBetween("2026-04-01", "2026-04-30")).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/measures/by-date-range")
                                .param("from", "2026-04-01")
                                .param("to", "2026-04-30"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].sensorId").value("CAP-001"));

                verify(measureQueryService).getMeasuresBetween("2026-04-01", "2026-04-30");
        }

        @Test
        @DisplayName("GET /api/measures/by-date-range retourne 400 pour un format de date invalide")
        void getMeasuresByDateRangeShouldReturn400ForInvalidDate() throws Exception {
                when(measureQueryService.getMeasuresBetween(any(), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Invalid date format, expected yyyy-MM-dd"));

                mockMvc.perform(get("/api/measures/by-date-range")
                                .param("from", "invalid")
                                .param("to", "2026-04-30"))
                                .andExpect(status().isBadRequest());
        }

        private MeasureDto buildDto() {
                return new MeasureDto(
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "MES-001",
                                OffsetDateTime.parse("2026-04-13T10:15:30+02:00"),
                                42.5f,
                                "ppm",
                                "CAP-001",
                                49.1829,
                                -0.3707,
                                "ZONE-001",
                                "TYPE-001");
        }
}
