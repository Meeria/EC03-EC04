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

@Entity
@Table(name = "zone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Zone {

    @Id
    @Column(name = "uuid", columnDefinition = "UUID")
    @EqualsAndHashCode.Include
    private UUID uuid = UUID.randomUUID();

    @Column(name = "zone_id", nullable = false)
    private String zoneId;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "zone_sensor",
        joinColumns = @JoinColumn(name = "zone_uuid"),
        inverseJoinColumns = @JoinColumn(name = "sensor_uuid")
    )
    private Set<Sensor> sensors = new HashSet<>();
}
