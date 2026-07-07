package caensup.eadl.urbanhub.ingest.service;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;

/**
 * Point d'entrée unique pour l'ingestion de mesures, quelle que soit la source (HTTP ou MQTT).
 */
public interface MeasureIngestService {

    /**
     * Valide et persiste une mesure.
     *
     * <p>Si le capteur ({@code sensor_id}) ou son type ({@code type}) n'existent pas encore en base,
     * ils sont créés automatiquement à la volée.
     *
     * @param json le payload brut désérialisé
     * @throws caensup.eadl.urbanhub.ingest.exception.InvalidMeasureException si la mesure est invalide
     */
    void ingestMeasure(IngestMeasureJson json);

}