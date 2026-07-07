package caensup.eadl.urbanhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.UUID;

import java.util.Set;
import java.time.Instant;

/**
 * Capteur physique identifié par un UUID interne et un identifiant fonctionnel ({@code sensor_id}).
 * Créé automatiquement à la première ingestion si absent de la base.
 */
@Entity
@Table(name = "sensor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sensor {

    @Id
    @Column(name = "uuid", columnDefinition = "UUID")
    @EqualsAndHashCode.Include
    private UUID uuid = UUID.randomUUID();

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @ManyToMany(mappedBy = "sensors")
    private Set<Zone> zones = new HashSet<>();

    @Column(name = "last_update", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private Instant lastUpdate = Instant.now();

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne
    @JoinColumn(name = "sensor_type", nullable = false)
    private SensorType sensorType;

    @OneToMany(mappedBy = "sensor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Measure> measures;

    // Retourne la zone principale associée au capteur (si relations ManyToMany), ou null si aucune.
    public caensup.eadl.urbanhub.entity.Zone getPrimaryZone() {
        if (zones == null || zones.isEmpty()) return null;
        // renvoie une zone arbitraire (première) — adapte selon vos règles métier
        return zones.iterator().next();
    }

    @PrePersist
    public void prePersist() {
        if (lastUpdate == null) {
            lastUpdate = Instant.now();
        }
    }
}
