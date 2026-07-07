package caensup.eadl.urbanhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import caensup.eadl.urbanhub.dto.MeasureDto;
import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.entity.MeasureId;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.entity.Zone;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MeasureQueryServiceTest {

    @Mock
    private MeasureRepository measureRepository;

    private MeasureQueryService measureQueryService;

    @BeforeEach
    void setUp() {
        measureQueryService = new MeasureQueryService(measureRepository);
    }

    @Test
    @DisplayName("getMeasures sans filtre retourne toutes les mesures")
    void getMeasuresShouldReturnAllMeasuresWhenNoFilterIsProvided() {
        Measure measure = buildMeasure();
        when(measureRepository.findAll()).thenReturn(List.of(measure));

        List<MeasureDto> result = measureQueryService.getMeasures(null);

        assertEquals(1, result.size());
        verify(measureRepository).findAll();
    }

    @Test
    @DisplayName("getMeasures avec sensor_id filtre par capteur")
    void getMeasuresShouldFilterBySensorId() {
        Measure measure = buildMeasure();
        when(measureRepository.findBySensor_SensorId("CAP-001")).thenReturn(List.of(measure));

        List<MeasureDto> result = measureQueryService.getMeasures("CAP-001");

        assertEquals(1, result.size());
        verify(measureRepository).findBySensor_SensorId("CAP-001");
    }

    @Test
    @DisplayName("getCount retourne le nombre total de mesures")
    void getCountShouldReturnRepositoryCount() {
        when(measureRepository.count()).thenReturn(100L);

        long result = measureQueryService.getCount();

        assertEquals(100L, result);
        verify(measureRepository).count();
    }

    @Test
    @DisplayName("getMeasuresByDay interroge le repo avec début et fin de journée")
    void getMeasuresByDayShouldQueryCorrectRange() {
        Measure measure = buildMeasure();
        when(measureRepository.findBetween(any(), any())).thenReturn(List.of(measure));

        List<MeasureDto> result = measureQueryService.getMeasuresByDay("2026-04-13");

        assertEquals(1, result.size());

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(measureRepository).findBetween(fromCaptor.capture(), toCaptor.capture());

        assertEquals(0, fromCaptor.getValue().getHour());
        assertEquals(0, fromCaptor.getValue().getMinute());
        assertEquals(23, toCaptor.getValue().getHour());
        assertEquals(59, toCaptor.getValue().getMinute());
    }

    @Test
    @DisplayName("getMeasuresByDay lève 400 pour un format de date invalide")
    void getMeasuresByDayShouldThrowBadRequestForInvalidDate() {
        assertThrows(ResponseStatusException.class, () -> measureQueryService.getMeasuresByDay("invalid-date"));
    }

    @Test
    @DisplayName("getMeasuresBetween interroge le repo avec la plage de dates correcte")
    void getMeasuresBetweenShouldQueryCorrectRange() {
        Measure measure = buildMeasure();
        when(measureRepository.findBetween(any(), any())).thenReturn(List.of(measure));

        List<MeasureDto> result = measureQueryService.getMeasuresBetween("2026-04-01", "2026-04-30");

        assertEquals(1, result.size());

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(measureRepository).findBetween(fromCaptor.capture(), toCaptor.capture());

        assertEquals(1, fromCaptor.getValue().getDayOfMonth());
        assertEquals(30, toCaptor.getValue().getDayOfMonth());
    }

    @Test
    @DisplayName("getMeasuresBetween lève 400 pour un format de date invalide")
    void getMeasuresBetweenShouldThrowBadRequestForInvalidDate() {
        assertThrows(ResponseStatusException.class, () -> measureQueryService.getMeasuresBetween("invalid", "2026-04-30"));
    }

    private Measure buildMeasure() {
        Zone zone = new Zone();
        zone.setUuid(UUID.randomUUID());
        zone.setZoneId("ZONE-001");
        Set<Zone> zones = new HashSet<>();
        zones.add(zone);

        SensorType sensorType = new SensorType();
        sensorType.setUuid(UUID.randomUUID());
        sensorType.setSensorTypeId("TYPE-001");

        Sensor sensor = new Sensor();
        sensor.setUuid(UUID.randomUUID());
        sensor.setSensorId("CAP-001");
        sensor.setLatitude(49.1829);
        sensor.setLongitude(-0.3707);
        sensor.setZones(zones);
        sensor.setSensorType(sensorType);

        Measure measure = new Measure();
        MeasureId id = new MeasureId(OffsetDateTime.parse("2026-04-13T10:15:30+02:00"), sensor.getUuid());
        measure.setId(id);
        measure.setValue(42.5f);
        measure.setUnit("ppm");
        measure.setSensor(sensor);
        return measure;
    }
}
