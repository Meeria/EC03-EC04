package caensup.eadl.urbanhub.ingest.exception;

public class ZoneAlreadyExistsException extends RuntimeException {

    private final String zoneId;

    public ZoneAlreadyExistsException(String zoneId) {
        super("Zone already exists: " + zoneId);
        this.zoneId = zoneId;
    }

    public String getZoneId() {
        return zoneId;
    }
}
