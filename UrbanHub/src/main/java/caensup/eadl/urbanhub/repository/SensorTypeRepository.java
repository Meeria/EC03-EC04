package caensup.eadl.urbanhub.repository;

import caensup.eadl.urbanhub.entity.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorTypeRepository extends JpaRepository<SensorType, UUID> {

    /**
     * Finds a sensor type by its business identifier (e.g. "AIR", "NOISE"...).
     */
    Optional<SensorType> findBySensorTypeId(String sensorTypeId);
}
