package caensup.eadl.urbanhub.repository;

import caensup.eadl.urbanhub.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    /**
     * Recherche une zone par son identifiant métier.
     */
    Optional<Zone> findByZoneId(String zoneId);

    boolean existsByZoneId(String zoneId);
}
