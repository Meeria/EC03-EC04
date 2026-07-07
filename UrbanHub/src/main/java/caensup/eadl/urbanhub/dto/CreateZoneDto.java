package caensup.eadl.urbanhub.dto;

import java.util.List;

public record CreateZoneDto(
    String zoneId,
    List<String> sensorIds
) {}