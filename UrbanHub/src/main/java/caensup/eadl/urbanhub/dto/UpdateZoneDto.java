package caensup.eadl.urbanhub.dto;

import java.util.List;

public record UpdateZoneDto(
    String zoneId,
    List<String> sensorIds
) {}
