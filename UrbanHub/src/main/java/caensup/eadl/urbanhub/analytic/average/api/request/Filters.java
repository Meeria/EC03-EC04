package caensup.eadl.urbanhub.analytic.average.api.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Filters
 */
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class Filters {
    List<String> type;
    List<String> zoneIds;
    LocalDateTime debut;
    LocalDateTime fin;
}
