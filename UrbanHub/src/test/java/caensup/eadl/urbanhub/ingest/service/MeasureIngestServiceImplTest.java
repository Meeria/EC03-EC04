package caensup.eadl.urbanhub.ingest.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import caensup.eadl.urbanhub.repository.SensorRepository;
import caensup.eadl.urbanhub.repository.SensorTypeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeasureIngestServiceImplTest {

    @Mock
    private MeasureRepository measureRepository;
    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorTypeRepository sensorTypeRepository;

    private MeasureIngestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MeasureIngestServiceImpl(measureRepository, sensorRepository, sensorTypeRepository);
    }

    @Test
    @DisplayName("ingestMeasure utilise le capteur existant sans en créer un nouveau")
    void shouldUseExistingSensorWhenFound() {
        Sensor existing = new Sensor();
        existing.setSensorId("CAP-001");

        when(sensorRepository.findBySensorId("CAP-001")).thenReturn(Optional.of(existing));
        when(measureRepository.save(any())).thenReturn(new Measure());

        service.ingestMeasure(new IngestMeasureJson("CAP-001", "air", "1744538100000", "49.18;-0.37", 25.0, "\u03bcg/m3"));

        verify(sensorRepository).findBySensorId("CAP-001");
        verify(sensorRepository).save(any());
        verify(measureRepository).save(any());
    }

    @Test
    @DisplayName("ingestMeasure crée un capteur avec le type existant quand le capteur est inconnu")
    void shouldCreateSensorWithExistingTypeWhenSensorNotFound() {
        SensorType existingType = new SensorType();
        existingType.setSensorTypeId("AIR");

        when(sensorRepository.findBySensorId("CAP-NEW")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("AIR")).thenReturn(Optional.of(existingType));
        when(sensorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(measureRepository.save(any())).thenReturn(new Measure());

        service.ingestMeasure(new IngestMeasureJson("CAP-NEW", "air", "1744538100000", "49.18;-0.37", 25.0, "\u03bcg/m3"));

        verify(sensorTypeRepository).findBySensorTypeId("AIR");
        verify(sensorTypeRepository, never()).save(any());
        verify(sensorRepository, times(2)).save(any());
        verify(measureRepository).save(any());
    }

    @Test
    @DisplayName("ingestMeasure crée le type et le capteur quand ni l'un ni l'autre n'existe")
    void shouldCreateSensorTypeAndSensorWhenNeitherExist() {
        SensorType savedType = new SensorType();
        savedType.setSensorTypeId("AIR");

        when(sensorRepository.findBySensorId("CAP-NEW")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("AIR")).thenReturn(Optional.empty());
        when(sensorTypeRepository.save(any())).thenReturn(savedType);
        when(sensorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(measureRepository.save(any())).thenReturn(new Measure());

        service.ingestMeasure(new IngestMeasureJson("CAP-NEW", "air", "1744538100000", null, 25.0, "\u03bcg/m3"));

        verify(sensorTypeRepository).findBySensorTypeId("AIR");
        verify(sensorTypeRepository).save(any());
        verify(sensorRepository, times(2)).save(any());
        verify(measureRepository).save(any());
    }

    @Test
    @DisplayName("ingestMeasure parse correctement les coordonnées depuis la localisation")
    void shouldParseCoordinatesFromLocation() {
        SensorType sensorType = new SensorType();

        when(sensorRepository.findBySensorId("CAP-LOC")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("NOISE")).thenReturn(Optional.of(sensorType));
        when(sensorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(measureRepository.save(any())).thenReturn(new Measure());

        service.ingestMeasure(new IngestMeasureJson("CAP-LOC", "noise", "1744538100000", "49.18;-0.37", 50.0, "dB"));

        verify(sensorRepository, times(2)).save(argThat(s ->
                s.getLatitude() == 49.18 && s.getLongitude() == -0.37
        ));
    }

    @ParameterizedTest(name = "location={0}")
    @NullSource
    @ValueSource(strings = {"invalid-location"})
    @DisplayName("ingestMeasure utilise 0.0 pour les coordonnées si la localisation est null ou sans point-virgule")
    void shouldDefaultToZeroCoordinatesForInvalidLocation(String location) {
        SensorType sensorType = new SensorType();

        when(sensorRepository.findBySensorId("CAP-BAD")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("NOISE")).thenReturn(Optional.of(sensorType));
        // Le service utilise la valeur retournée par save() : renvoyer l'entité passée (avec lat/lon déjà posées).
        when(sensorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(measureRepository.save(any())).thenReturn(new Measure());

        service.ingestMeasure(new IngestMeasureJson("CAP-BAD", "noise", "1744538100000", location, 50.0, "dB"));

        verify(sensorRepository, times(2)).save(argThat(s ->
                s.getLatitude() == 0.0 && s.getLongitude() == 0.0
        ));
    }

    @Test
    @DisplayName("ingestMeasure lève InvalidMeasureException si les coordonnées ne sont pas des nombres")
    void shouldThrowWhenLocationHasNonNumericParts() {
        when(sensorRepository.findBySensorId("CAP-NAN")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("NOISE")).thenReturn(Optional.of(new SensorType()));

        IngestMeasureJson json = new IngestMeasureJson("CAP-NAN", "noise", "1744538100000", "abc;xyz", 50.0, "dB");
        assertThrows(InvalidMeasureException.class, () -> service.ingestMeasure(json));
    }
}
