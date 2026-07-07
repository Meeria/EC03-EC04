package caensup.eadl.urbanhub.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestServiceImpl;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import caensup.eadl.urbanhub.repository.SensorRepository;
import caensup.eadl.urbanhub.repository.SensorTypeRepository;

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

    // -------------------------------------------------------------------------
    // Sensor resolution
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sensor existant : aucune création, mesure sauvegardée")
    void ingestMeasure_sensorExists_noCreation() {
        Sensor existing = sensorWithId("CAP-001");
        when(sensorRepository.findBySensorId("CAP-001")).thenReturn(Optional.of(existing));

        service.ingestMeasure(validWeatherJson("CAP-001"));

        verify(sensorRepository).save(any());
        verify(measureRepository).save(any());
    }

    @Test
    @DisplayName("Sensor absent, SensorType existant : sensor créé avec le type existant")
    void ingestMeasure_sensorNotFound_typeExists_createsSensor() {
        SensorType type = sensorType("WEATHER");
        when(sensorRepository.findBySensorId("CAP-NEW")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("WEATHER")).thenReturn(Optional.of(type));
        when(sensorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestMeasure(validWeatherJson("CAP-NEW"));

        verify(sensorTypeRepository, never()).save(any());
        verify(sensorRepository, times(2)).save(argThat(s -> "CAP-NEW".equals(s.getSensorId())));
        verify(measureRepository).save(any());
    }

    @Test
    @DisplayName("Sensor absent, SensorType absent : les deux sont créés")
    void ingestMeasure_sensorAndTypeNotFound_createsBoth() {
        when(sensorRepository.findBySensorId("CAP-NEW")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("WEATHER")).thenReturn(Optional.empty());
        when(sensorTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sensorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestMeasure(validWeatherJson("CAP-NEW"));

        verify(sensorTypeRepository).save(argThat(t -> "WEATHER".equals(t.getSensorTypeId())));
        verify(sensorRepository, times(2)).save(any());
        verify(measureRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // Location parsing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Location valide : latitude et longitude parsées correctement")
    void ingestMeasure_validLocation_parsedCorrectly() {
        when(sensorRepository.findBySensorId("CAP-LOC")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("WEATHER")).thenReturn(Optional.of(sensorType("WEATHER")));
        when(sensorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IngestMeasureJson json = new IngestMeasureJson("CAP-LOC", "weather", "1718236800000", "49.1828; -0.3706", 18.5, "°C");
        service.ingestMeasure(json);

        verify(sensorRepository, times(2)).save(argThat(s ->
                s.getLatitude() == 49.1828 && s.getLongitude() == -0.3706
        ));
    }

    @Test
    @DisplayName("Location nulle : lat/lon par défaut à 0.0")
    void ingestMeasure_nullLocation_defaultCoordinates() {
        when(sensorRepository.findBySensorId("CAP-NULL")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("WEATHER")).thenReturn(Optional.of(sensorType("WEATHER")));
        when(sensorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IngestMeasureJson json = new IngestMeasureJson("CAP-NULL", "weather", "1718236800000", null, 18.5, "°C");
        service.ingestMeasure(json);

        verify(sensorRepository, times(2)).save(argThat(s ->
                s.getLatitude() == 0.0 && s.getLongitude() == 0.0
        ));
    }

    @Test
    @DisplayName("Location mal formée : InvalidMeasureException levée")
    void ingestMeasure_invalidLocationFormat_throwsException() {
        when(sensorRepository.findBySensorId("CAP-BAD")).thenReturn(Optional.empty());
        when(sensorTypeRepository.findBySensorTypeId("WEATHER")).thenReturn(Optional.of(sensorType("WEATHER")));

        IngestMeasureJson json = new IngestMeasureJson("CAP-BAD", "weather", "1718236800000", "not;a;valid;location", 18.5, "°C");

        assertThrows(InvalidMeasureException.class, () -> service.ingestMeasure(json));
        verify(measureRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Measure validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unité invalide pour le type : InvalidMeasureException levée avant tout accès repo")
    void ingestMeasure_invalidUnit_throwsBeforeRepoAccess() {
        IngestMeasureJson json = new IngestMeasureJson("CAP-001", "weather", "1718236800000", null, 18.5, "km/h");

        assertThrows(InvalidMeasureException.class, () -> service.ingestMeasure(json));

        verify(sensorRepository, never()).findBySensorId(any());
        verify(measureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Valeur nulle : InvalidMeasureException levée avant tout accès repo")
    void ingestMeasure_nullValue_throwsBeforeRepoAccess() {
        IngestMeasureJson json = new IngestMeasureJson("CAP-001", "weather", "1718236800000", null, null, "°C");

        assertThrows(InvalidMeasureException.class, () -> service.ingestMeasure(json));

        verify(sensorRepository, never()).findBySensorId(any());
        verify(measureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Type inconnu : IllegalArgumentException levée avant tout accès repo")
    void ingestMeasure_unknownType_throwsBeforeRepoAccess() {
        IngestMeasureJson json = new IngestMeasureJson("CAP-001", "unknown", "1718236800000", null, 18.5, "°C");

        assertThrows(IllegalArgumentException.class, () -> service.ingestMeasure(json));

        verify(sensorRepository, never()).findBySensorId(any());
        verify(measureRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Sensor sensorWithId(String sensorId) {
        Sensor s = new Sensor();
        s.setSensorId(sensorId);
        s.setLatitude(0.0);
        s.setLongitude(0.0);
        // UUID already set by default in the entity
        return s;
    }

    private SensorType sensorType(String typeId) {
        SensorType t = new SensorType();
        t.setSensorTypeId(typeId);
        return t;
    }

    private IngestMeasureJson validWeatherJson(String sensorId) {
        return new IngestMeasureJson(sensorId, "weather", "1718236800000", null, 18.5, "°C");
    }
}
