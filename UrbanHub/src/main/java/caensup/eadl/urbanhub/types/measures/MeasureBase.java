package caensup.eadl.urbanhub.types.measures;

import java.time.Instant;

import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;

/**
 * Base class common to all measures from a sensor.
 * Common fields are here; each subclass provides
 * its {@link MeasureType} and business validation rules.
 */
public abstract class MeasureBase {

    private final String sensorId;
    private final Instant timestamp;
    private final String location;
    private final Double value;
    private final String unit;

    protected MeasureBase(String sensorId, Instant timestamp, String location, Double value, String unit) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.location = location;
        this.value = value;
        this.unit = unit;
    }

    public String sensorId()    { return sensorId; }
    public Instant timestamp()  { return timestamp; }
    public String location()    { return location; }
    public Double value()       { return value; }
    public String unit()        { return unit; }

    public abstract MeasureType type();

    /**
     * Checks that the measure is coherent.
     * Throws {@link InvalidMeasureException} if a rule is violated.
     */
    public abstract void validate();

    /** Shared utility: rejects a missing or too-far-in-the-future timestamp. */
    protected void validateTimestamp() {
        if (timestamp == null || timestamp.isAfter(Instant.now().plusSeconds(300))) {
            throw new InvalidMeasureException("timestamp is missing or in the future");
        }
    }
}
