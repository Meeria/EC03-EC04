package caensup.eadl.urbanhub.dto;

import java.util.UUID;

public record SensorDto(
    UUID uuid,
    String sensorId,
    String sensorTypeId,
    Double latitude,
    Double longitude,
    Boolean status,
    String zoneId
) {}