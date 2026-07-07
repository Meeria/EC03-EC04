package caensup.eadl.urbanhub.ingest.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier en réponses HTTP RFC 7807 ({@link org.springframework.http.ProblemDetail}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidMeasureException.class)
	public ResponseEntity<ProblemDetail> handleInvalidMeasure(InvalidMeasureException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		detail.setTitle("Invalid measure");
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(detail);
	}

	@ExceptionHandler(SensorNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleSensorNotFound(SensorNotFoundException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		detail.setTitle("Sensor not found");
		detail.setProperty("sensorId", ex.getSensorId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
	}

	@ExceptionHandler(SensorTypeNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleSensorTypeNotFound(SensorTypeNotFoundException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		detail.setTitle("Sensor type not found");
		detail.setProperty("sensorTypeId", ex.getSensorTypeId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
	}

	@ExceptionHandler(ZoneNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleZoneNotFound(ZoneNotFoundException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		detail.setTitle("Zone not found");
		detail.setProperty("zoneId", ex.getZoneId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
	}

	@ExceptionHandler(ZoneAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleZoneAlreadyExists(ZoneAlreadyExistsException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		detail.setTitle("Zone already exists");
		detail.setProperty("zoneId", ex.getZoneId());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
		detail.setTitle("Invalid request");
		return ResponseEntity.badRequest().body(detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableJson(HttpMessageNotReadableException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "Unreadable or malformed JSON body");
		detail.setTitle("Invalid request");
		return ResponseEntity.badRequest().body(detail);
	}
}
