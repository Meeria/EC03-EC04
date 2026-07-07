package caensup.eadl.urbanhub.repository;

import caensup.eadl.urbanhub.entity.Sensor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    Optional<Sensor> findBySensorId(String sensorId);

    List<Sensor> findByLastUpdateGreaterThanEqual(Instant cutoff);

    List<Sensor> findByLastUpdateLessThan(Instant cutoff);

    @EntityGraph(attributePaths = { "sensorType", "zones" })
    List<Sensor> findAll();

    @EntityGraph(attributePaths = { "sensorType", "zones" })
    List<Sensor> findBySensorType_SensorTypeId(String sensorTypeId);
}
