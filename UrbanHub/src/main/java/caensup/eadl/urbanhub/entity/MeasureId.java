package caensup.eadl.urbanhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Composite key for the TimescaleDB "measure" table.
 * TimescaleDB requires the temporal partition column (timestamp) to be part of the primary key.
 *
 * Natural uniqueness: a sensor can only emit one measure at a given instant.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeasureId implements Serializable {

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "sensor_uuid", columnDefinition = "UUID", nullable = false)
    private UUID sensorUuid;
}
