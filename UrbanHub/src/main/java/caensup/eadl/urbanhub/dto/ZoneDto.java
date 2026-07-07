package caensup.eadl.urbanhub.dto;

import java.util.UUID;

public record ZoneDto(
    UUID uuid,
    String zoneId,
    java.util.List<SensorDto> sensors
) {}