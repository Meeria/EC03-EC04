package caensup.eadl.urbanhub.analytic.trend.api.dto;

/**
 * Variation par rapport à une mesure de référence (ex. N-1, N-24h).
 */
public record TrendDelta(Float changeAbsolute, Float changePercent, String comparedTo) {}
