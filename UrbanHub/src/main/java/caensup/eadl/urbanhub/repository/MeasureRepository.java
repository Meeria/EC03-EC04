package caensup.eadl.urbanhub.repository;

import caensup.eadl.urbanhub.analytic.average.projection.KpiAverageProjection;
import caensup.eadl.urbanhub.entity.Measure;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeasureRepository extends JpaRepository<Measure, UUID> {

    /**
     * Retrieves all measure records from the database with their associated relationships eagerly loaded.
     * <p>
     * This method uses an {@link EntityGraph} to perform a "fetch join" on the sensor,
     * the sensor's zone, and the sensor's type. This prevents the "N+1 selects problem"
     * by retrieving the measure and its full hierarchical context in a single optimized SQL query.
     * </p>
     * * @return A {@link List} of {@link Measure} entities containing fully initialized
     * sensor, zone, and sensor type data.
     *
     * @see EntityGraph
     */
    @Override
    @NotNull
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    List<Measure> findAll();

    /**
     * Finds measures whose timestamp falls within [from, to].
     */
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    @Query("SELECT m FROM Measure m WHERE m.id.timestamp >= :from AND m.id.timestamp <= :to")
    List<Measure> findBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    // New helper query methods for trend calculations

    /**
     * Returns the most recent measure for a sensor.
     */
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    List<Measure> findBySensor_SensorId(String sensorId);


    /**
     * Calcule la moyenne globale pour un type de capteur donné (ex: 'NO2')
     * sur une période précise.
     */
    @Query(value = """
            SELECT
                time_bucket(CAST(:bucket AS interval), m.timestamp) AS bucket,
                AVG(m.value) AS average,
                m.unit AS unit
                    FROM measure m
            JOIN sensor s ON m.sensor_uuid = s.uuid
            JOIN sensor_type st ON s.sensor_type = st.uuid
            WHERE st.sensor_type_id = :sensorTypeId
            AND m.timestamp >= :startTime
            AND m.timestamp <= :endTime
            GROUP BY
                        bucket,unit
                    ORDER BY
                        bucket ASC
            """, nativeQuery = true)
    List<KpiAverageProjection> getAverageBySensorTypeId(
            @Param("sensorTypeId") String sensorTypeId,
            @Param("bucket") String bucket,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query(value = """
            SELECT
            time_bucket(CAST(:bucket AS interval), m.timestamp) AS bucket,
            AVG(m.value) AS average,
                m.unit AS unit
            FROM measure m
            JOIN sensor s ON m.sensor_uuid = s.uuid
            JOIN sensor_type st ON s.sensor_type = st.uuid
            JOIN zone_sensor zs ON s.uuid = zs.sensor_uuid
            JOIN zone z ON zs.zone_uuid = z.uuid
            WHERE st.sensor_type_id = :sensorTypeId
            AND z.zone_id = :zoneId
            AND m.timestamp >= :startTime
            AND m.timestamp <= :endTime
            GROUP BY
                        bucket,unit
                    ORDER BY
                        bucket ASC
            """, nativeQuery = true)
    List<KpiAverageProjection> getAverageByZoneId(
            @Param("zoneId") String zoneId,
            @Param("bucket") String bucket,
            @Param("sensorTypeId") String sensorTypeId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query(value = """
            SELECT
                time_bucket(CAST(:bucket AS interval), m.timestamp) AS bucket,
                AVG(m.value) AS average,
                m.unit AS unit
                        FROM measure m
            JOIN sensor s ON m.sensor_uuid = s.uuid
            WHERE s.sensor_id = :sensorId
            AND m.timestamp >= :startTime
            AND m.timestamp <= :endTime
            GROUP BY
                        bucket,unit
                    ORDER BY
                        bucket ASC
            """, nativeQuery = true)
    List<KpiAverageProjection> getAverageBySensorId(
            @Param("sensorId") String sensorId,
            @Param("bucket") String bucket,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    Optional<Measure> findTopBySensor_SensorIdOrderById_TimestampDesc(String sensorId);

    /**
     * Returns the N most recent measures for a sensor ordered descending by timestamp.
     */
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    List<Measure> findTop2BySensor_SensorIdOrderById_TimestampDesc(String sensorId);

    /**
     * Returns the latest measure with timestamp <= given timestamp.
     */
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    Optional<Measure> findTopBySensor_SensorIdAndId_TimestampLessThanEqualOrderById_TimestampDesc(String sensorId, OffsetDateTime ts);

    /**
     * Returns the earliest measure with timestamp >= given timestamp.
     */
    @EntityGraph(attributePaths = {"sensor", "sensor.zones", "sensor.sensorType"})
    Optional<Measure> findTopBySensor_SensorIdAndId_TimestampGreaterThanEqualOrderById_TimestampAsc(String sensorId, OffsetDateTime ts);
}