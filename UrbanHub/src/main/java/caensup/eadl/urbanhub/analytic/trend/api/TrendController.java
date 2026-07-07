package caensup.eadl.urbanhub.analytic.trend.api;

import caensup.eadl.urbanhub.analytic.trend.api.dto.TrendDto;
import caensup.eadl.urbanhub.analytic.trend.api.service.TrendCalculationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trends")
public class TrendController {

    private final TrendCalculationService trendCalculationService;

    public TrendController(TrendCalculationService trendCalculationService) {
        this.trendCalculationService = trendCalculationService;
    }

    @GetMapping("/sensor/latest-vs-previous")
    public TrendDto latestVsPrevious(@RequestParam("sensor_id") String sensorId) {
        Optional<TrendDto> t = trendCalculationService.computeTrendLatestVsPrevious(sensorId);
        return t.orElse(null);
    }

    @GetMapping("/sensor/latest-vs-24h")
    public TrendDto latestVs24h(@RequestParam("sensor_id") String sensorId) {
        Optional<TrendDto> t = trendCalculationService.computeTrendLatestVs24h(sensorId);
        return t.orElse(null);
    }

    @GetMapping("/zone/period")
    public List<TrendDto> zonePeriod(
            @RequestParam("zone_id") String zoneId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return trendCalculationService.computeTrendsByZoneInPeriod(zoneId, start, end);
    }

    @GetMapping("/sensor/period")
    public TrendDto sensorPeriod(
            @RequestParam("sensor_id") String sensorId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        Optional<TrendDto> t = trendCalculationService.computeTrendForSensorInPeriod(sensorId, start, end);
        return t.orElse(null);
    }

    @GetMapping("/period")
    public List<TrendDto> allSensorsPeriod(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return trendCalculationService.computeTrendsInPeriod(start, end);
    }
}
