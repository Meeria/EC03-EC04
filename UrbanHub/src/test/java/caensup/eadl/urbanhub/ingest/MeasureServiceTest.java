package caensup.eadl.urbanhub.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;
import caensup.eadl.urbanhub.types.measures.MeasureBase;
import caensup.eadl.urbanhub.types.measures.MeasureFactory;

class MeasureServiceTest {

	@ParameterizedTest
	@CsvSource({
		"weather, °C",
		"air,     μg/m3",
		"noise,   dB",
		"traffic, km/h"
	})
	@DisplayName("Ingestion d'une mesure de type {0} avec unité {1} est valide")
	void ingestMeasureValid(String type, String unit) {
		IngestMeasureJson ingestMeasureJson = new IngestMeasureJson("1234567890", type, "78976865754", "1234567890", 20.0, unit);
		MeasureBase measure = MeasureFactory.from(ingestMeasureJson);
		measure.validate();
		assertEquals(20.0, ingestMeasureJson.value());
	}

	@ParameterizedTest
	@CsvSource({
		"weather, km/h",
		"air,     °C",
		"noise,   μg/m3",
		"traffic, dB"
	})
	@DisplayName("Ingestion d'une mesure de type {0} avec unité {1} est invalide")
	void ingestMeasureInvalid(String type, String unit) {
		IngestMeasureJson ingestMeasureJson = new IngestMeasureJson("1234567890", type, "78976865754", "1234567890", 20.0, unit);
		assertThrows(InvalidMeasureException.class, () -> MeasureFactory.from(ingestMeasureJson));
	}

	@Test
	@DisplayName("Ingestion d'une mesure avec une valeur nulle est invalide")
	void ingestMeasureNullValue() {
		IngestMeasureJson ingestMeasureJson = new IngestMeasureJson("1234567890", "weather", "78976865754", "1234567890", null, "°C");
		MeasureBase measure = MeasureFactory.from(ingestMeasureJson);
		assertThrows(InvalidMeasureException.class, measure::validate);
	}

	@Test
	@DisplayName("Ingestion d'une mesure avec une valeur négative est valide")
	void ingestMeasureNegativeValueValid() {
		IngestMeasureJson ingestMeasureJson = new IngestMeasureJson("1234567890", "weather", "78976865754", "1234567890", -1.0, "°C");
		MeasureBase measure = MeasureFactory.from(ingestMeasureJson);
		measure.validate();
		assertEquals(-1.0, ingestMeasureJson.value());
	}

	@Test
	@DisplayName("Ingestion d'une mesure avec une valeur avec type inconnu est invalide")
	void ingestMeasureUnknownTypeInvalid() {
		IngestMeasureJson ingestMeasureJson = new IngestMeasureJson("1234567890", "unknown", "78976865754", "1234567890", 20.0, "°C");
		assertThrows(IllegalArgumentException.class, () -> MeasureFactory.from(ingestMeasureJson));
	}

}
