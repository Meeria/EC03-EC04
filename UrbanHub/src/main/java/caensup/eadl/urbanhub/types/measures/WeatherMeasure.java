package caensup.eadl.urbanhub.types.measures;

import java.time.Instant;

import caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException;

/**
 * Mesure météorologique exprimée en degrés Celsius (°C).
 *
 * <p>
 * Règles métier :
 * <ul>
 * <li>L'unité fournie doit être exactement {@code "°C"}.</li>
 * <li>La valeur doit être présente (non {@code null}) — les températures
 * négatives sont autorisées.</li>
 * <li>Le timestamp ne doit pas être dans le futur.</li>
 * </ul>
 */
public class WeatherMeasure extends MeasureBase {

    private static final String UNIT = "°C";

	/**
	 * Crée une mesure météorologique.
	 *
	 * @param sensorId   identifiant du capteur
	 * @param timestamp  instant de la mesure
	 * @param location   lieu de la mesure
	 * @param value      valeur météo mesurée (°C)
	 * @param unitString unité déclarée dans le message entrant ; doit être
	 *                   {@code "°C"}
	 * @throws InvalidMeasureException si {@code unitString} ne correspond pas à
	 *                                 {@code "°C"}
	 */
	public WeatherMeasure(String sensorId, Instant timestamp, String location, Double value, String unitString) {
		super(sensorId, timestamp, location, value, UNIT);

		if (!UNIT.equals(unitString)) {
			throw new InvalidMeasureException("Value must be in " + UNIT + " (received: " + unitString + ")");
		}
	}

	/** @return {@link MeasureType#WEATHER} */
	@Override
	public MeasureType type() {
		return MeasureType.WEATHER;
	}

	/**
	 * Vérifie que la valeur météo est présente, puis valide le timestamp.
	 *
	 * <p>
	 * Contrairement aux autres mesures, les valeurs négatives sont acceptées
	 * (ex. températures hivernales sous zéro).
	 *
	 * @throws InvalidMeasureException si la valeur est {@code null},
	 *                                 ou si le timestamp est absent / dans le futur
	 */
	@Override
	public void validate() {
		if (value() == null) {
			throw new InvalidMeasureException("la valeur météo est absente");
		}
		validateTimestamp();
	}
}
