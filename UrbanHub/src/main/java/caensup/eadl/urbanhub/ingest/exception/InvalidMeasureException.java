package caensup.eadl.urbanhub.ingest.exception;

public class InvalidMeasureException extends RuntimeException {

	public InvalidMeasureException(String reason) {
		super("Invalid measure: " + reason);
	}
}
