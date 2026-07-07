package caensup.eadl.urbanhub.analytic.average.api;

import caensup.eadl.urbanhub.analytic.average.api.request.MeasureTypeKpiRequest;
import caensup.eadl.urbanhub.analytic.average.api.request.SensorIdKpiRequest;
import caensup.eadl.urbanhub.analytic.average.api.request.ZoneIdKpiRequest;
import caensup.eadl.urbanhub.analytic.average.model.KPI;
import caensup.eadl.urbanhub.analytic.average.service.CalculationKPIService;
import caensup.eadl.urbanhub.types.measures.MeasureType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationKPIControllerTest {

    @Mock
    private CalculationKPIService calculationKPIService;

    @InjectMocks
    private CalculationKPIController controller;

    @Test
    void getKPIbyType_ShouldConvertParametersAndCallService() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 15, 10, 0);
        MeasureTypeKpiRequest request = new MeasureTypeKpiRequest("NOISE", start, end, "1 day");

        List<KPI> expectedResponse = List.of(new KPI(start.toInstant(ZoneOffset.UTC), 20.0, "µg/m3"));

        when(calculationKPIService.getKPIbyType(MeasureType.NOISE, "1 day", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC)))
                .thenReturn(expectedResponse);

        // Act
        List<KPI> response = controller.getKPIbyType(request);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(calculationKPIService).getKPIbyType(MeasureType.NOISE, "1 day", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC));
    }

    @Test
    void getKPIbyZone_ShouldConvertParametersAndCallService() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 15, 10, 0);
        ZoneIdKpiRequest request = new ZoneIdKpiRequest("1", start, end, "1 hour");

        Map<String, List<KPI>> expectedResponse = Map.of("NOISE", Collections.emptyList());

        when(calculationKPIService.getKPIbyZone("1", "1 hour", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, List<KPI>> response = controller.getKPIbyZone(request);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(calculationKPIService).getKPIbyZone("1", "1 hour", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC));
    }

    @Test
    void getKPIbySensor_ShouldConvertParametersAndCallService() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 15, 10, 0);
        SensorIdKpiRequest request = new SensorIdKpiRequest("SENSOR-XYZ", start, end, "1 day");

        List<KPI> expectedResponse = List.of(new KPI(start.toInstant(ZoneOffset.UTC), 45.0, "dB"));

        when(calculationKPIService.getKPIbySensorId("SENSOR-XYZ", "1 day", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC)))
                .thenReturn(expectedResponse);

        // Act
        List<KPI> response = controller.getKPIbySensor(request);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(calculationKPIService).getKPIbySensorId("SENSOR-XYZ", "1 day", start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC));
    }

}

