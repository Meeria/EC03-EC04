package caensup.eadl.urbanhub.types.measures;

/**
 * Catégories de mesures reconnues par le pipeline d'ingestion.
 * Chaque valeur correspond à une sous-classe de {@link MeasureBase} avec ses propres règles de validation.
 */
public enum MeasureType {
	TRAFFIC,
	AIR,
	NOISE,
	WEATHER
}
