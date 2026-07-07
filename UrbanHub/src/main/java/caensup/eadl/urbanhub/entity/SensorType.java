package caensup.eadl.urbanhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sensor_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SensorType {

    @Id
    @Column(name = "uuid", columnDefinition = "UUID")
    @EqualsAndHashCode.Include
    private UUID uuid = UUID.randomUUID();

    @Column(name = "sensor_type_id", nullable = false)
    private String sensorTypeId;

    @OneToMany(mappedBy = "sensorType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sensor> sensors;
}
