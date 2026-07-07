package caensup.eadl.urbanhub.analytic.average.api.request;

import java.time.LocalDateTime;

public record SensorIdKpiRequest(
        String sensorId,
        LocalDateTime start,
        LocalDateTime end,
        String bucket
) {
}
