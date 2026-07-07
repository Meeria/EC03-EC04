package caensup.eadl.urbanhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import caensup.eadl.urbanhub.dto.SensorDto;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.entity.Zone;
import caensup.eadl.urbanhub.ingest.exception.SensorNotFoundException;
import caensup.eadl.urbanhub.repository.SensorRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    private SensorService sensorService;

    @BeforeEach
    void setUp() {
        sensorService = new SensorService(sensorRepository);
    }

    @Test
    @DisplayName("getAll retourne les capteurs mappés avec statut vivant si last_update dans la fenêtre")
    void getAllShouldMapSensorsWithAliveStatus() {
        Sensor alive = buildSensor("S1", "AIR", Instant.now().minus(30, ChronoUnit.MINUTES), null);
        Sensor dead = buildSensor("S2", "NOISE", Instant.now().minus(3, ChronoUnit.HOURS), null);
        when(sensorRepository.findAll()).thenReturn(List.of(alive, dead));

        List<SensorDto> result = sensorService.getAll();

        assertEquals(2, result.size());
        assertTrue(result.stream().filter(d -> d.sensorId().equals("S1")).findFirst().orElseThrow().status());
        assertFalse(result.stream().filter(d -> d.sensorId().equals("S2")).findFirst().orElseThrow().status());
        verify(sensorRepository).findAll();
    }

    @Test
    @DisplayName("getByType filtre par type de capteur")
    void getByTypeShouldReturnSensorsForType() {
        Sensor s = buildSensor("S1", "AIR", Instant.now(), null);
        when(sensorRepository.findBySensorType_SensorTypeId("AIR")).thenReturn(List.of(s));

        List<SensorDto> result = sensorService.getByType("AIR");

        assertEquals(1, result.size());
        assertEquals("AIR", result.get(0).sensorTypeId());
        verify(sensorRepository).findBySensorType_SensorTypeId("AIR");
    }

    @Test
    @DisplayName("getByStatus(true) utilise les capteurs dont last_update >= cutoff")
    void getByStatusTrueShouldUseGreaterThanEqualCutoff() {
        Sensor s = buildSensor("S1", "AIR", Instant.now(), null);
        when(sensorRepository.findByLastUpdateGreaterThanEqual(any(Instant.class))).thenReturn(List.of(s));

        List<SensorDto> result = sensorService.getByStatus(true);

        assertEquals(1, result.size());
        assertTrue(result.get(0).status());
        verify(sensorRepository).findByLastUpdateGreaterThanEqual(any(Instant.class));
    }

    @Test
    @DisplayName("getByStatus(false) utilise les capteurs dont last_update < cutoff")
    void getByStatusFalseShouldUseLessThanCutoff() {
        Sensor s = buildSensor("S1", "AIR", Instant.now().minus(3, ChronoUnit.HOURS), null);
        when(sensorRepository.findByLastUpdateLessThan(any(Instant.class))).thenReturn(List.of(s));

        List<SensorDto> result = sensorService.getByStatus(false);

        assertEquals(1, result.size());
        assertFalse(result.get(0).status());
        verify(sensorRepository).findByLastUpdateLessThan(any(Instant.class));
    }

    @Test
    @DisplayName("getByStatusCount retourne la taille de la liste filtrée par statut")
    void getByStatusCountShouldReturnListSize() {
        Sensor s = buildSensor("S1", "AIR", Instant.now(), null);
        when(sensorRepository.findByLastUpdateGreaterThanEqual(any(Instant.class))).thenReturn(List.of(s));

        long count = sensorService.getByStatusCount(true);

        assertEquals(1L, count);
        verify(sensorRepository).findByLastUpdateGreaterThanEqual(any(Instant.class));
    }

    @Test
    @DisplayName("getCount délègue au dépôt")
    void getCountShouldReturnRepositoryCount() {
        when(sensorRepository.count()).thenReturn(7L);

        long result = sensorService.getCount();

        assertEquals(7L, result);
        verify(sensorRepository).count();
    }

    @Test
    @DisplayName("getById retourne le DTO quand le capteur existe")
    void getByIdShouldReturnDtoWhenPresent() {
        UUID uuid = UUID.randomUUID();
        Sensor s = buildSensor("S1", "AIR", Instant.now(), null);
        s.setUuid(uuid);
        when(sensorRepository.findBySensorId("S1")).thenReturn(Optional.of(s));

        SensorDto dto = sensorService.getById("S1");

        assertEquals(uuid, dto.uuid());
        assertEquals("S1", dto.sensorId());
        assertEquals("AIR", dto.sensorTypeId());
        assertTrue(dto.status());
        verify(sensorRepository).findBySensorId("S1");
    }

    @Test
    @DisplayName("getById lève SensorNotFoundException si le capteur est absent")
    void getByIdShouldThrowWhenMissing() {
        when(sensorRepository.findBySensorId("UNKNOWN")).thenReturn(Optional.empty());

        SensorNotFoundException ex =
                assertThrows(SensorNotFoundException.class, () -> sensorService.getById("UNKNOWN"));

        assertEquals("UNKNOWN", ex.getSensorId());
    }

    @Test
    @DisplayName("toDto expose la première zone quand la collection zones est renseignée")
    void dtoShouldExposeFirstZoneIdWhenZonesPresent() {
        Zone z = new Zone();
        z.setZoneId("Z-01");
        Set<Zone> zones = new LinkedHashSet<>();
        zones.add(z);
        Sensor s = buildSensor("S1", "AIR", Instant.now(), zones);

        when(sensorRepository.findAll()).thenReturn(List.of(s));

        SensorDto dto = sensorService.getAll().get(0);

        assertEquals("Z-01", dto.zoneId());
    }

    @Test
    @DisplayName("toDto renvoie zoneId null si zones est null ou vide")
    void dtoShouldHaveNullZoneIdWhenNoZones() {
        Sensor noZones = buildSensor("S1", "AIR", Instant.now(), null);
        Sensor emptyZones = buildSensor("S2", "AIR", Instant.now(), Set.of());
        when(sensorRepository.findAll()).thenReturn(List.of(noZones, emptyZones));

        List<SensorDto> result = sensorService.getAll();

        assertNull(result.get(0).zoneId());
        assertNull(result.get(1).zoneId());
    }

    private Sensor buildSensor(String sensorId, String typeId, Instant lastUpdate, Set<Zone> zones) {
        SensorType sensorType = new SensorType();
        sensorType.setSensorTypeId(typeId);
        Sensor sensor = new Sensor();
        sensor.setUuid(UUID.randomUUID());
        sensor.setSensorId(sensorId);
        sensor.setLatitude(49.18);
        sensor.setLongitude(-0.37);
        sensor.setLastUpdate(lastUpdate);
        sensor.setSensorType(sensorType);
        sensor.setZones(zones);
        return sensor;
    }
}
