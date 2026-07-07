package caensup.eadl.urbanhub.analytic.average.api;

import caensup.eadl.urbanhub.analytic.average.api.request.MeasureTypeKpiRequest;
import caensup.eadl.urbanhub.analytic.average.api.request.SensorIdKpiRequest;
import caensup.eadl.urbanhub.analytic.average.api.request.ZoneIdKpiRequest;
import caensup.eadl.urbanhub.analytic.average.model.KPI;
import caensup.eadl.urbanhub.analytic.average.service.CalculationKPIService;
import caensup.eadl.urbanhub.types.measures.MeasureType;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/analytic/kpi/average")
public class CalculationKPIController {
    private final CalculationKPIService calculationKPIService;

    public CalculationKPIController(CalculationKPIService calculationKPIService) {
        this.calculationKPIService = calculationKPIService;
    }

    /**
     * Retrieves a list of Key Performance Indicators (KPIs) filtered by a specific measure type.
     * <p>
     * This endpoint processes a request to calculate average values (KPIs) for a given
     * environmental or traffic metric (e.g., NO2, SPEED) over a specified period,
     * aggregated by a time bucket (e.g., '1 hour', '1 day').
     * </p>
     *
     * @param measureTypeKpiRequest An object containing:
     *                              <ul>
     *                              <li><b>measureType</b>: The string representation of the {@link MeasureType}</li>
     *                              <li><b>bucket</b>: The TimeScaleDB time interval (e.g., "1 hour")</li>
     *                              <li><b>start</b>: The start date and time (LocalDateTime)</li>
     *                              <li><b>end</b>: The end date and time (LocalDateTime)</li>
     *                              </ul>
     * @return A {@link List} of {@link KPI} objects containing the aggregated results.
     * @throws IllegalArgumentException if the provided measureType does not match any valid {@link MeasureType}.
     */
    @GetMapping("/bytype")
    public List<KPI> getKPIbyType(@NotNull MeasureTypeKpiRequest measureTypeKpiRequest) {
        MeasureType type;
        try {
            type = MeasureType.valueOf(measureTypeKpiRequest.measureType().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Le type reçu n'existe pas dans l'enum (ex: 'TEST')
            return List.of();
        }
        return calculationKPIService.getKPIbyType(type, measureTypeKpiRequest.bucket(), measureTypeKpiRequest.start().toInstant(ZoneOffset.UTC), measureTypeKpiRequest.end().toInstant(ZoneOffset.UTC));
    }

    /**
     * Retrieves a categorized map of KPIs for all available measure types within a specific zone.
     * <p>
     * This endpoint calculates the average values for every supported {@link MeasureType}
     * (e.g., air quality, traffic, noise) associated with the sensors located in the specified zone.
     * The results are grouped by measure type in the returning map.
     * </p>
     *
     * @param zoneIdKpiRequest A request object containing:
     *                         <ul>
     *                         <li><b>zoneId</b>: The unique identifier (String or UUID) of the geographic area.</li>
     *                         <li><b>bucket</b>: The time interval for data aggregation (e.g., "15 minutes", "1 day").</li>
     *                         <li><b>start</b>: The beginning of the observation period (LocalDateTime).</li>
     *                         <li><b>end</b>: The end of the observation period (LocalDateTime).</li>
     *                         </ul>
     * @return A {@link Map} where each key is a {@link MeasureType} name and the value is
     * a {@link List} of {@link KPI} aggregated data points for that specific type.
     */
    @GetMapping("/byzone")
    public Map<String, List<KPI>> getKPIbyZone(@NotNull ZoneIdKpiRequest zoneIdKpiRequest) {
        return calculationKPIService.getKPIbyZone(zoneIdKpiRequest.zoneId(), zoneIdKpiRequest.bucket(), zoneIdKpiRequest.start().toInstant(ZoneOffset.UTC), zoneIdKpiRequest.end().toInstant(ZoneOffset.UTC));
    }

    /**
     * Retrieves a list of KPIs for a specific individual sensor.
     * <p>
     * This endpoint fetches granular data for a single sensor identified by its unique ID.
     * It calculates the average measurements over the requested time period, aggregated
     * according to the provided time bucket interval.
     * </p>
     *
     * @param sensorIdKpiRequest A request object containing:
     *                           <ul>
     *                           <li><b>sensorId</b>: The unique identifier of the target sensor (e.g., UUID or String).</li>
     *                           <li><b>bucket</b>: The aggregation time interval (e.g., "5 minutes", "1 hour").</li>
     *                           <li><b>start</b>: The inclusive start date and time of the data range.</li>
     *                           <li><b>end</b>: The inclusive end date and time of the data range.</li>
     *                           </ul>
     * @return A {@link List} of {@link KPI} objects representing the aggregated values for this specific sensor.
     */
    @GetMapping("/bysensor")
    public List<KPI> getKPIbySensor(@NotNull SensorIdKpiRequest sensorIdKpiRequest) {
        return calculationKPIService.getKPIbySensorId(sensorIdKpiRequest.sensorId(), sensorIdKpiRequest.bucket(), sensorIdKpiRequest.start().toInstant(ZoneOffset.UTC), sensorIdKpiRequest.end().toInstant(ZoneOffset.UTC));
    }
}