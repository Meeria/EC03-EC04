package caensup.eadl.urbanhub.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initialise la hypertable TimescaleDB pour la table "mesure" au démarrage.
 *
 * Hibernate (ddl-auto=update) crée la table en premier, puis ce composant
 * appelle create_hypertable(). Le flag if_not_exists => TRUE rend l'opération
 * idempotente : aucune erreur si la hypertable existe déjà.
 *
 * Prérequis : l'extension TimescaleDB doit être installée sur la base PostgreSQL.
 *   CREATE EXTENSION IF NOT EXISTS timescaledb;
 */
@Component
public class TimescaleDbInitializer {

    private static final Logger log = LoggerFactory.getLogger(TimescaleDbInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public TimescaleDbInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initHypertable() {
        try {
            jdbcTemplate.execute(
                "SELECT create_hypertable('measure', by_range('timestamp'), if_not_exists => TRUE)"
            );
            log.info("TimescaleDB: hypertable 'measure' initialized (or already exists).");
        } catch (Exception e) {
            log.error("TimescaleDB: FAILED to initialize hypertable 'measure'. Measures will be stored as regular rows. Verify that the TimescaleDB extension is installed. Detail: {}", e.getMessage());
        }
    }
}
