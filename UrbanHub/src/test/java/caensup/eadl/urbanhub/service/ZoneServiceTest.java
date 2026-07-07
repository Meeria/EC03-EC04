package caensup.eadl.urbanhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.invocation.InvocationOnMock;


import caensup.eadl.urbanhub.dto.CreateZoneDto;

import caensup.eadl.urbanhub.dto.UpdateZoneDto;
import caensup.eadl.urbanhub.dto.ZoneDto;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.entity.Zone;
import caensup.eadl.urbanhub.ingest.exception.ZoneAlreadyExistsException;
import caensup.eadl.urbanhub.ingest.exception.ZoneNotFoundException;
import caensup.eadl.urbanhub.repository.SensorRepository;
import caensup.eadl.urbanhub.repository.ZoneRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private SensorRepository sensorRepository;

    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneService = new ZoneService(zoneRepository, sensorRepository);
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTests {
        @Test
        @DisplayName("retourne toutes les zones")
        void getAllShouldReturnAllZones() {
            when(zoneRepository.findAll()).thenReturn(List.of(buildZone("CENTRE"), buildZone("NORD")));

            List<ZoneDto> result = zoneService.getAll();

            assertEquals(2, result.size());
            assertEquals("CENTRE", result.get(0).zoneId());
            assertEquals("NORD", result.get(1).zoneId());
            verify(zoneRepository).findAll();
        }

        @Test
        @DisplayName("retourne une liste vide si aucune zone")
        void getAllShouldReturnEmptyListWhenNoZones() {
            when(zoneRepository.findAll()).thenReturn(List.of());

            List<ZoneDto> result = zoneService.getAll();

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {
        @Test
        @DisplayName("retourne la zone correspondante")
        void getByIdShouldReturnZone() {
            when(zoneRepository.findByZoneId("CENTRE")).thenReturn(Optional.of(buildZone("CENTRE")));

            ZoneDto result = zoneService.getById("CENTRE");

            assertEquals("CENTRE", result.zoneId());
            verify(zoneRepository).findByZoneId("CENTRE");
        }

        @Test
        @DisplayName("lève ZoneNotFoundException si la zone est introuvable")
        void getByIdShouldThrowNotFoundWhenZoneDoesNotExist() {
            when(zoneRepository.findByZoneId("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(ZoneNotFoundException.class, () -> zoneService.getById("UNKNOWN"));
        }
    }

    @Nested
    @DisplayName("getCount")
    class GetCountTests {
        @Test
        @DisplayName("retourne le nombre total de zones")
        void getCountShouldReturnRepositoryCount() {
            when(zoneRepository.count()).thenReturn(5L);

            long result = zoneService.getCount();

            assertEquals(5L, result);
            verify(zoneRepository).count();
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        @DisplayName("crée une zone sans capteurs")
        void createShouldCreateZoneWithoutSensors() {
            Zone saved = buildZone("CENTRE");
            when(zoneRepository.existsByZoneId("CENTRE")).thenReturn(false);
            when(zoneRepository.save(any(Zone.class))).thenReturn(saved);
            when(zoneRepository.findById(saved.getUuid())).thenReturn(Optional.of(saved));

            ZoneDto result = zoneService.create(new CreateZoneDto("CENTRE", List.of()));

            assertNotNull(result);
            assertEquals("CENTRE", result.zoneId());
            assertEquals(0, result.sensors().size());
            verify(zoneRepository).save(any(Zone.class));
        }

        @Test
        @DisplayName("crée une zone avec capteurs associés")
        void createShouldCreateZoneWithSensors() {
            Sensor sensor = buildSensor("SENSOR_01", "AIR");

            // Capture the entity that gets saved so we can mock findById to return it
            final Zone[] capturedZone = new Zone[1];

            when(zoneRepository.existsByZoneId("CENTRE")).thenReturn(false);
            when(sensorRepository.findBySensorId("SENSOR_01")).thenReturn(Optional.of(sensor));
            when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
                Zone z = inv.getArgument(0);
                z.getSensors().add(sensor);
                capturedZone[0] = z;
                return z;
            });
            when(zoneRepository.findById(any(UUID.class))).thenAnswer(inv -> {
                UUID id = inv.getArgument(0);
                if (capturedZone[0] != null && capturedZone[0].getUuid().equals(id)) {
                    return Optional.of(capturedZone[0]);
                }
                return Optional.of(buildZone("UNEXPECTED"));
            });

            ZoneDto result = zoneService.create(new CreateZoneDto("CENTRE", List.of("SENSOR_01")));

            assertNotNull(result);
            assertEquals("CENTRE", result.zoneId());
            assertEquals(1, result.sensors().size());
            assertEquals("SENSOR_01", result.sensors().get(0).sensorId());
        }

        @Test
        @DisplayName("lève ZoneAlreadyExistsException si la zone existe déjà")
        void createShouldThrowWhenZoneAlreadyExists() {
            when(zoneRepository.existsByZoneId("CENTRE")).thenReturn(true);

            assertThrows(ZoneAlreadyExistsException.class,
                    () -> zoneService.create(new CreateZoneDto("CENTRE", List.of())));
        }

        @Test
        @DisplayName("ignore les sensorIds qui n'existent pas en base")
        void createShouldIgnoreNonExistentSensorIds() {
            Zone saved = buildZone("CENTRE");
            when(zoneRepository.existsByZoneId("CENTRE")).thenReturn(false);
            when(sensorRepository.findBySensorId("UNKNOWN_SENSOR")).thenReturn(Optional.empty());
            when(zoneRepository.save(any(Zone.class))).thenReturn(saved);
            when(zoneRepository.findById(saved.getUuid())).thenReturn(Optional.of(saved));

            ZoneDto result = zoneService.create(new CreateZoneDto("CENTRE", List.of("UNKNOWN_SENSOR")));

            assertEquals("CENTRE", result.zoneId());
            assertEquals(0, result.sensors().size());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {
        @Test
        @DisplayName("met à jour le nom de la zone")
        void updateShouldRenameZone() {
            Zone zone = buildZone("OLD_NAME");
            Zone saved = buildZone("NEW_NAME");
            when(zoneRepository.findByZoneId("OLD_NAME")).thenReturn(Optional.of(zone));
            when(zoneRepository.existsByZoneId("NEW_NAME")).thenReturn(false);
            when(zoneRepository.save(any(Zone.class))).thenReturn(saved);
            when(zoneRepository.findById(saved.getUuid())).thenReturn(Optional.of(saved));

            ZoneDto result = zoneService.update("OLD_NAME", new UpdateZoneDto("NEW_NAME", List.of()));

            assertEquals("NEW_NAME", result.zoneId());
        }

        @Test
        @DisplayName("met à jour les capteurs de la zone")
        void updateShouldReplaceSensors() {
            Sensor sensorB = buildSensor("SENSOR_B", "NOISE");
            Zone zone = buildZone("CENTRE");
            when(zoneRepository.findByZoneId("CENTRE")).thenReturn(Optional.of(zone));
            when(sensorRepository.findBySensorId("SENSOR_B")).thenReturn(Optional.of(sensorB));
            // Capture what gets saved, then return it from findById
            when(zoneRepository.save(any(Zone.class))).thenAnswer((InvocationOnMock inv) -> {
                Zone z = inv.getArgument(0);
                z.getSensors().add(sensorB);
                return z;
            });
            when(zoneRepository.findById(any(UUID.class))).thenAnswer((InvocationOnMock inv) -> {
                zone.getSensors().add(sensorB);
                return Optional.of(zone);
            });

            ZoneDto result = zoneService.update("CENTRE", new UpdateZoneDto(null, List.of("SENSOR_B")));

            assertEquals("CENTRE", result.zoneId());
            assertEquals(1, result.sensors().size());
            assertEquals("SENSOR_B", result.sensors().get(0).sensorId());
        }

        @Test
        @DisplayName("lève ZoneNotFoundException si la zone n'existe pas")
        void updateShouldThrowWhenZoneNotFound() {
            when(zoneRepository.findByZoneId("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(ZoneNotFoundException.class,
                    () -> zoneService.update("UNKNOWN", new UpdateZoneDto("X", List.of())));
        }

        @Test
        @DisplayName("lève ZoneAlreadyExistsException si le nouveau nom est déjà pris")
        void updateShouldThrowWhenNewNameAlreadyExists() {
            Zone zone = buildZone("CENTRE");
            when(zoneRepository.findByZoneId("CENTRE")).thenReturn(Optional.of(zone));
            when(zoneRepository.existsByZoneId("NORD")).thenReturn(true);

            assertThrows(ZoneAlreadyExistsException.class,
                    () -> zoneService.update("CENTRE", new UpdateZoneDto("NORD", List.of())));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {
        @Test
        @DisplayName("supprime la zone existante")
        void deleteShouldDeleteZone() {
            Zone zone = buildZone("CENTRE");
            when(zoneRepository.findByZoneId("CENTRE")).thenReturn(Optional.of(zone));

            zoneService.delete("CENTRE");

            verify(zoneRepository).delete(zone);
        }

        @Test
        @DisplayName("lève ZoneNotFoundException si la zone n'existe pas")
        void deleteShouldThrowWhenZoneNotFound() {
            when(zoneRepository.findByZoneId("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(ZoneNotFoundException.class, () -> zoneService.delete("UNKNOWN"));
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private Zone buildZone(String zoneId) {
        Zone zone = new Zone();
        zone.setUuid(UUID.randomUUID());
        zone.setZoneId(zoneId);
        return zone;
    }

    private Sensor buildSensor(String sensorId, String typeId) {
        SensorType type = new SensorType();
        type.setUuid(UUID.randomUUID());
        type.setSensorTypeId(typeId);

        Sensor sensor = new Sensor();
        sensor.setUuid(UUID.randomUUID());
        sensor.setSensorId(sensorId);
        sensor.setSensorType(type);
        sensor.setLatitude(49.0);
        sensor.setLongitude(-0.4);
        return sensor;
    }
}