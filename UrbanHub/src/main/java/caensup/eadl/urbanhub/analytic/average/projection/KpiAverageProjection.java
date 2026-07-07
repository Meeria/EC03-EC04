package caensup.eadl.urbanhub.analytic.average.projection;

import java.time.Instant;

public interface KpiAverageProjection {
    Instant getBucket();

    Double getAverage();

    String getUnit();
}