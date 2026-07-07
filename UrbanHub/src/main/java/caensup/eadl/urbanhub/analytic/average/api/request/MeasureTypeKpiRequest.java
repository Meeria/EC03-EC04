package caensup.eadl.urbanhub.analytic.average.api.request;

import java.time.LocalDateTime;

public record MeasureTypeKpiRequest(
        String measureType,
        LocalDateTime start,
        LocalDateTime end,
        String bucket
) {
}