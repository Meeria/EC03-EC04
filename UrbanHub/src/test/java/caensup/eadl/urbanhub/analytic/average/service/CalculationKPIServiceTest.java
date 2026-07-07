package caensup.eadl.urbanhub.analytic.average.service;

import caensup.eadl.urbanhub.analytic.average.model.KPI;
import caensup.eadl.urbanhub.analytic.average.projection.KpiAverageProjection;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import caensup.eadl.urbanhub.types.measures.MeasureType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculationKPIServiceTest {

    @Mock
    private MeasureRepository measureRepository;

    @InjectMocks
    private CalculationKPIService calculationKPIService;

    @Test
    void getKPIbyType_ShouldMapProjectionToKPI() {
        // Arrange
        Instant start = Instant.parse("2026-04-01T00:00:00Z");
        Instant end = Instant.parse("2026-04-15T00:00:00Z");
        String bucket = "1 day";

        KpiAverageProjection mockProjection = mock(KpiAverageProjection.class);
        when(mockProjection.getBucket()).thenReturn(start);
        when(mockProjection.getAverage()).thenReturn(42.5);
        when(mockProjection.getUnit()).thenReturn("µg/m3");

        when(measureRepository.getAverageBySensorTypeId(MeasureType.NOISE.name(), bucket, start, end))
                .thenReturn(List.of(mockProjection));

        // Act
        List<KPI> result = calculationKPIService.getKPIbyType(MeasureType.NOISE, bucket, start, end);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBucket()).isEqualTo(start);
        assertThat(result.getFirst().getAverage()).isEqualTo(42.5);
        assertThat(result.getFirst().getUnite().equals("µg/m3"));
        verify(measureRepository).getAverageBySensorTypeId(MeasureType.NOISE.name(), bucket, start, end);
    }

    @Test
    void getKPIbyZone_ShouldIterateOverAllMeasureTypes() {
        // Arrange
        Instant start = Instant.parse("2026-04-01T00:00:00Z");
        Instant end = Instant.parse("2026-04-15T00:00:00Z");
        String zoneId = "ZONE-CENTRE";
        String bucket = "1 hour";

        KpiAverageProjection mockProjection = mock(KpiAverageProjection.class);
        when(mockProjection.getBucket()).thenReturn(start);
        when(mockProjection.getAverage()).thenReturn(50.0);
        when(mockProjection.getUnit()).thenReturn("km/h");

        // Simule un retour uniquement pour le type SPEED, et vide pour les autres
        when(measureRepository.getAverageByZoneId(eq(zoneId), eq(bucket), any(String.class), eq(start), eq(end)))
                .thenAnswer(invocation -> {
                    String type = invocation.getArgument(2);
                    if (MeasureType.TRAFFIC.name().equals(type)) {
                        return List.of(mockProjection);
                    }
                    return List.of();
                });

        // Act
        Map<String, List<KPI>> result = calculationKPIService.getKPIbyZone(zoneId, bucket, start, end);

        // Assert
        assertThat(result).containsOnlyKeys(
                java.util.Arrays.stream(MeasureType.values()).map(Enum::name).toArray(String[]::new)
        );
        assertThat(result.get(MeasureType.TRAFFIC.name())).hasSize(1);
        assertThat(result.get(MeasureType.NOISE.name())).isEmpty(); // Test dynamique basé sur l'enum

        // Vérifie que le repo a été appelé pour chaque type de l'enum
        verify(measureRepository, times(MeasureType.values().length))
                .getAverageByZoneId(eq(zoneId), eq(bucket), any(String.class), eq(start), eq(end));
    }

    @Test
    void getKPIbySensorId_ShouldMapProjectionToKPI() {
        // Arrange
        Instant start = Instant.parse("2026-04-01T00:00:00Z");
        Instant end = Instant.parse("2026-04-15T00:00:00Z");
        String sensorId = "RADAR-01";

        KpiAverageProjection mockProjection = mock(KpiAverageProjection.class);
        when(mockProjection.getAverage()).thenReturn(80.0);

        when(measureRepository.getAverageBySensorId(sensorId, "1 hour", start, end))
                .thenReturn(List.of(mockProjection));

        // Act
        List<KPI> result = calculationKPIService.getKPIbySensorId(sensorId, "1 hour", start, end);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAverage()).isEqualTo(80.0);
    }
}