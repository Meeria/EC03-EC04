package caensup.eadl.urbanhub.analytic.average.api.request;

import java.time.LocalDateTime;

public record ZoneIdKpiRequest(
        String zoneId,
        LocalDateTime start,
        LocalDateTime end,
        String bucket
) {
}
