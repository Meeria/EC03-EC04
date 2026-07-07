package caensup.eadl.urbanhub.analytic.average.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class KPI {
    Instant bucket;
    Double average;
    String unite;

}
