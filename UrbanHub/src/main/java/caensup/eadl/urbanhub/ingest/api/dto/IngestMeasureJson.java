package caensup.eadl.urbanhub.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngestMeasureJson(
		@NotBlank @JsonProperty("sensor_id") String sensorId,
		@NotBlank String type,
		@NotBlank String timestamp,
		String location,
		@NotNull Double value,
		@NotBlank String unit) {
}
