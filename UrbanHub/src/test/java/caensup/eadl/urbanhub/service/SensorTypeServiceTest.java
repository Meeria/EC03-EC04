package caensup.eadl.urbanhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import caensup.eadl.urbanhub.dto.SensorTypeDto;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.ingest.exception.SensorTypeNotFoundException;
import caensup.eadl.urbanhub.repository.SensorTypeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensorTypeServiceTest {

    @Mock
    private SensorTypeRepository sensorTypeRepository;

    private SensorTypeService sensorTypeService;

    @BeforeEach
    void setUp() {
        sensorTypeService = new SensorTypeService(sensorTypeRepository);
    }

    @Test
    @DisplayName("getAll retourne tous les types de capteur")
    void getAllShouldReturnAllSensorTypes() {
        when(sensorTypeRepository.findAll()).thenReturn(List.of(buildSensorType("AIR"), buildSensorType("NOISE")));

        List<SensorTypeDto> result = sensorTypeService.getAll();

        assertEquals(2, result.size());
        assertEquals("AIR", result.get(0).sensorTypeId());
        verify(sensorTypeRepository).findAll();
    }

    @Test
    @DisplayName("getById retourne le type de capteur correspondant")
    void getByIdShouldReturnSensorType() {
        when(sensorTypeRepository.findBySensorTypeId("AIR")).thenReturn(Optional.of(buildSensorType("AIR")));

        SensorTypeDto result = sensorTypeService.getById("AIR");

        assertEquals("AIR", result.sensorTypeId());
        verify(sensorTypeRepository).findBySensorTypeId("AIR");
    }

    @Test
    @DisplayName("getById lève SensorTypeNotFoundException si le type est introuvable")
    void getByIdShouldThrowNotFoundWhenTypeDoesNotExist() {
        when(sensorTypeRepository.findBySensorTypeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(SensorTypeNotFoundException.class, () -> sensorTypeService.getById("UNKNOWN"));
    }

    @Test
    @DisplayName("getCount retourne le nombre total de types de capteur")
    void getCountShouldReturnRepositoryCount() {
        when(sensorTypeRepository.count()).thenReturn(4L);

        long result = sensorTypeService.getCount();

        assertEquals(4L, result);
        verify(sensorTypeRepository).count();
    }

    private SensorType buildSensorType(String typeId) {
        SensorType sensorType = new SensorType();
        sensorType.setUuid(UUID.randomUUID());
        sensorType.setSensorTypeId(typeId);
        return sensorType;
    }
}
