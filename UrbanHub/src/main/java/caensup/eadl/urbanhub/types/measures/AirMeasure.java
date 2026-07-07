package caensup.eadl.urbanhub.types.measures;

import java.time.Instant;

import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;

/**
 * Mesure de qualité de l'air exprimée en microgrammes par mètre cube (μg/m³).
 *
 * <p>
 * Règles métier :
 * <ul>
 * <li>L'unité fournie doit être exactement {@code "μg/m3"} (U+03BC).</li>
 * <li>La valeur doit être {@code >= 0} — une concentration ne peut pas être
 * négative.</li>
 * <li>Le timestamp ne doit pas être dans le futur.</li>
 * </ul>
 */
public class AirMeasure extends MeasureBase {

	private static final String UNIT = "μg/m3";

	/**
	 * Crée une mesure de qualité de l'air.
	 *
	 * @param sensorId   identifiant du capteur
	 * @param timestamp  instant de la mesure
	 * @param location   lieu de la mesure
	 * @param value      concentration mesurée (μg/m³)
	 * @param unitString unité déclarée dans le message entrant ; doit être
	 *                   {@code "μg/m3"}
	 * @throws InvalidMeasureException si {@code unitString} ne correspond pas à
	 *                                 {@code "μg/m3"}
	 */
	public AirMeasure(String sensorId, Instant timestamp, String location, Double value, String unitString) {
		super(sensorId, timestamp, location, value, UNIT);

		if (!UNIT.equals(unitString)) {
			throw new InvalidMeasureException("Value must be in " + UNIT + " (received: " + unitString + ")");
		}
	}

	/** @return {@link MeasureType#AIR} */
	@Override
	public MeasureType type() {
		return MeasureType.AIR;
	}

	/**
	 * Vérifie que la concentration est présente et non négative, puis valide le
	 * timestamp.
	 *
	 * @throws InvalidMeasureException si la valeur est {@code null} ou {@code < 0},
	 *                                 ou si le timestamp est absent / dans le futur
	 */
	@Override
	public void validate() {
		if (value() == null || value() < 0) {
			throw new InvalidMeasureException("la valeur de qualité de l'air doit être >= 0 (reçu: " + value() + ")");
		}
		validateTimestamp();
	}
}
