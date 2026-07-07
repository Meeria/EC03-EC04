package caensup.eadl.urbanhub.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO exposed for measure reads.
 */
public record MeasureDto(
        UUID uuid,
        String measureId,
        OffsetDateTime timestamp,
        Float value,
        String unit,
        String sensorId,
        Double latitude,
        Double longitude,
        String zoneId,
        String sensorTypeId
) {
}
