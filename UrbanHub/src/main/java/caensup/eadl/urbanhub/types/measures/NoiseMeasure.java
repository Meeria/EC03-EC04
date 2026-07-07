package caensup.eadl.urbanhub.types.measures;

import java.time.Instant;

import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;

/**
 * Mesure de niveau sonore exprimée en décibels (dB).
 *
 * <p>
 * Règles métier :
 * <ul>
 * <li>L'unité fournie doit être exactement {@code "dB"}.</li>
 * <li>La valeur doit être {@code >= 0} — un niveau sonore ne peut pas être
 * négatif.</li>
 * <li>Le timestamp ne doit pas être dans le futur.</li>
 * </ul>
 */
public class NoiseMeasure extends MeasureBase {

	private static final String UNIT = "dB";

	/**
	 * Crée une mesure de niveau sonore.
	 *
	 * @param sensorId   identifiant du capteur
	 * @param timestamp  instant de la mesure
	 * @param location   lieu de la mesure
	 * @param value      niveau sonore mesuré (dB)
	 * @param unitString unité déclarée dans le message entrant ; doit être
	 *                   {@code "dB"}
	 * @throws InvalidMeasureException si {@code unitString} ne correspond pas à
	 *                                 {@code "dB"}
	 */
	public NoiseMeasure(String sensorId, Instant timestamp, String location, Double value, String unitString) {
		super(sensorId, timestamp, location, value, UNIT);

		if (!UNIT.equals(unitString)) {
			throw new InvalidMeasureException("Value must be in " + UNIT + " (received: " + unitString + ")");
		}
	}

	/** @return {@link MeasureType#NOISE} */
	@Override
	public MeasureType type() {
		return MeasureType.NOISE;
	}

	/**
	 * Vérifie que le niveau sonore est présent et non négatif, puis valide le
	 * timestamp.
	 *
	 * @throws InvalidMeasureException si la valeur est {@code null} ou {@code < 0},
	 *                                 ou si le timestamp est absent / dans le futur
	 */
	@Override
	public void validate() {
		if (value() == null || value() < 0) {
			throw new InvalidMeasureException("le niveau sonore doit être >= 0 dB (reçu: " + value() + ")");
		}
		validateTimestamp();
	}
}
