package caensup.eadl.urbanhub.ingest.exception;

public class SensorTypeNotFoundException extends RuntimeException {

    private final String sensorTypeId;

    public SensorTypeNotFoundException(String sensorTypeId) {
        super("Sensor type not found: " + sensorTypeId);
        this.sensorTypeId = sensorTypeId;
    }

    public String getSensorTypeId() {
        return sensorTypeId;
    }
}
