package caensup.eadl.urbanhub.ingest.exception;

public class ZoneNotFoundException extends RuntimeException {

    private final String zoneId;

    public ZoneNotFoundException(String zoneId) {
        super("Zone not found: " + zoneId);
        this.zoneId = zoneId;
    }

    public String getZoneId() {
        return zoneId;
    }
}
