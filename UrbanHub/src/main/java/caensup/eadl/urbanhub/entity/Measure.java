package caensup.eadl.urbanhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TimescaleDB entity.
 * The composite primary key (timestamp, sensorUuid) is defined in {@link MeasureId}.
 * TimescaleDB partitions the table automatically by "timestamp".
 */
@Entity
@Table(name = "measure")
@Data
@NoArgsConstructor
public class Measure {

    @EmbeddedId
    private MeasureId id;

    /**
     * Links the sensorUuid field of the composite key to the Sensor FK.
     * insertable/updatable = false because the column is already managed by @EmbeddedId.
     */
    @MapsId("sensorUuid")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_uuid", insertable = false, updatable = false)
    private Sensor sensor;

    @Column(name = "\"value\"", nullable = false)
    private Float value;

    @Column(name = "unit", nullable = false)
    private String unit;
}
