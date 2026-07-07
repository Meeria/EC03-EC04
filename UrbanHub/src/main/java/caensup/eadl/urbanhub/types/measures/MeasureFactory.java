package caensup.eadl.urbanhub.types.measures;

import java.time.Instant;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;

/**
 * Transforms a raw DTO ({@link IngestMeasureJson}) into a domain object {@link MeasureBase}
 * according to the {@code type} field.
 */
public final class MeasureFactory {

    private MeasureFactory() {
    }

    public static MeasureBase from(IngestMeasureJson dto) {
        MeasureType type = parseType(dto.type());
        Instant ts = Instant.ofEpochMilli(Long.parseLong(dto.timestamp()));

        return switch (type) {
            case TRAFFIC -> new TrafficMeasure(
                    dto.sensorId(), ts, dto.location(), dto.value(), dto.unit());
            case AIR -> new AirMeasure(
                    dto.sensorId(), ts, dto.location(), dto.value(), dto.unit());
            case NOISE -> new NoiseMeasure(
                    dto.sensorId(), ts, dto.location(), dto.value(), dto.unit());
            case WEATHER -> new WeatherMeasure(
                    dto.sensorId(), ts, dto.location(), dto.value(), dto.unit());
        };
    }

    private static MeasureType parseType(String raw) {
        try {
            return MeasureType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown measure type: " + raw);
        }
    }
}
