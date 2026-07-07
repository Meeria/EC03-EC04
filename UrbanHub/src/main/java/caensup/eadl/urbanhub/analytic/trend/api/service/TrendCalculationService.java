package caensup.eadl.urbanhub.analytic.trend.api.service;

import caensup.eadl.urbanhub.analytic.trend.api.dto.TrendDelta;
import caensup.eadl.urbanhub.analytic.trend.api.dto.TrendDto;
import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service computing trends on demand. Results are not persisted.
 * Only computes differences (absolute and percent) — no averaging.
 */
@Service
public class TrendCalculationService {

    private final MeasureRepository measureRepository;

    public TrendCalculationService(MeasureRepository measureRepository) {
        this.measureRepository = measureRepository;
    }

    /**
     * Compute trends for a sensor comparing the latest measure against the previous (N vs N-1)
     */
    @Transactional(readOnly = true)
    public Optional<TrendDto> computeTrendLatestVsPrevious(String sensorId) {
        List<Measure> two = measureRepository.findTop2BySensor_SensorIdOrderById_TimestampDesc(sensorId);
        if (two == null || two.size() < 2) {
            return Optional.empty();
        }
        Measure latest = two.get(0);
        Measure prev = two.get(1);
        return Optional.of(buildTrend(latest, prev, "N-1"));
    }

    /**
     * Compute trend comparing latest measure to measure ~24 hours before.
     * The method finds the latest measure and then looks for the measure closest to timestamp -24h.
     */
    @Transactional(readOnly = true)
    public Optional<TrendDto> computeTrendLatestVs24h(String sensorId) {
        Optional<Measure> latestOpt = measureRepository.findTopBySensor_SensorIdOrderById_TimestampDesc(sensorId);
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }
        Measure latest = latestOpt.get();
        OffsetDateTime target = latest.getId().getTimestamp().minusHours(24);

        // try to find exact or nearest before/after
        Optional<Measure> before = measureRepository.findTopBySensor_SensorIdAndId_TimestampLessThanEqualOrderById_TimestampDesc(sensorId, target);
        Optional<Measure> after = measureRepository.findTopBySensor_SensorIdAndId_TimestampGreaterThanEqualOrderById_TimestampAsc(sensorId, target);

        Measure chosen = null;
        if (before.isPresent() && after.isPresent()) {
            // choose closest in time
            Duration dbefore = Duration.between(before.get().getId().getTimestamp(), target).abs();
            Duration dafter = Duration.between(after.get().getId().getTimestamp(), target).abs();
            chosen = dbefore.compareTo(dafter) <= 0 ? before.get() : after.get();
        } else if (before.isPresent()) {
            chosen = before.get();
        } else if (after.isPresent()) {
            chosen = after.get();
        }

        if (chosen == null) {
            return Optional.empty();
        }

        return Optional.of(buildTrend(latest, chosen, "N-24h"));
    }

    /**
     * Compute trends for all sensors in a zone within a time window: for each sensor, compare last measure in the window to the previous measure in the window.
     * Only differences are computed, no averaging.
     */
    @Transactional(readOnly = true)
    public List<TrendDto> computeTrendsByZoneInPeriod(String zoneId, OffsetDateTime start, OffsetDateTime end) {
        // Strategy: find for each sensor in the zone its first and last measure within the period.
        // We need to query sensors by zone. Reuse repository methods by sensor_id; here we will scan all measures in the period and group by sensor.

        // We'll fetch all measures between start and end by calling repository findAll() and filtering in memory to keep changes minimal.
        List<Measure> all = measureRepository.findAll();
        List<TrendDto> results = new ArrayList<>();

        // group measures by sensorId and zone
        var bySensor = new java.util.HashMap<String, List<Measure>>();
        for (Measure m : all) {
            if (m.getSensor() == null || m.getSensor().getPrimaryZone() == null) continue;
            String z = m.getSensor().getPrimaryZone().getZoneId();
            if (!zoneId.equals(z)) continue;
            OffsetDateTime ts = m.getId().getTimestamp();
            if (ts.isBefore(start) || ts.isAfter(end)) continue;
            String sid = m.getSensor().getSensorId();
            bySensor.computeIfAbsent(sid, k -> new ArrayList<>()).add(m);
        }

        for (var entry : bySensor.entrySet()) {
            List<Measure> measures = entry.getValue();
            measures.sort((a, b) -> b.getId().getTimestamp().compareTo(a.getId().getTimestamp())); // desc
            if (measures.size() < 2) continue; // need at least 2 to compute trend
            Measure latest = measures.get(0);
            Measure prev = measures.get(1);
            results.add(buildTrend(latest, prev, "period-last-vs-previous"));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public Optional<TrendDto> computeTrendForSensorInPeriod(String sensorId, OffsetDateTime start, OffsetDateTime end) {
        List<Measure> measures = measureRepository.findBySensor_SensorId(sensorId);
        if (measures == null || measures.isEmpty()) return Optional.empty();

        // filter by window
        List<Measure> inWindow = new ArrayList<>();
        for (Measure m : measures) {
            OffsetDateTime ts = m.getId().getTimestamp();
            if (ts == null) continue;
            if (!ts.isBefore(start) && !ts.isAfter(end)) {
                inWindow.add(m);
            }
        }
        if (inWindow.size() < 2) return Optional.empty();
        inWindow.sort((a, b) -> b.getId().getTimestamp().compareTo(a.getId().getTimestamp())); // desc
        Measure latest = inWindow.get(0);
        Measure prev = inWindow.get(1);
        return Optional.of(buildTrend(latest, prev, "period-N-1"));
    }

    @Transactional(readOnly = true)
    public List<TrendDto> computeTrendsInPeriod(OffsetDateTime start, OffsetDateTime end) {
        List<Measure> all = measureRepository.findAll();
        List<TrendDto> results = new ArrayList<>();

        var bySensor = new java.util.HashMap<String, List<Measure>>();
        for (Measure m : all) {
            if (m.getSensor() == null) continue;
            OffsetDateTime ts = m.getId().getTimestamp();
            if (ts == null) continue;
            if (ts.isBefore(start) || ts.isAfter(end)) continue;
            String sid = m.getSensor().getSensorId();
            bySensor.computeIfAbsent(sid, k -> new ArrayList<>()).add(m);
        }

        for (var entry : bySensor.entrySet()) {
            List<Measure> measures = entry.getValue();
            measures.sort((a, b) -> b.getId().getTimestamp().compareTo(a.getId().getTimestamp())); // desc
            if (measures.size() < 2) continue;
            Measure latest = measures.get(0);
            Measure prev = measures.get(1);
            results.add(buildTrend(latest, prev, "period-last-vs-previous"));
        }

        return results;
    }

    private TrendDto buildTrend(Measure latest, Measure previous, String comparedTo) {
        Float v = latest.getValue();
        Float pv = previous.getValue();
        Float abs = null;
        Float pct = null;
        if (v != null && pv != null) {
            abs = v - pv;
            if (pv != 0.0f) {
                pct = (abs / pv) * 100.0f;
            }
        }

        String zoneId = latest.getSensor() != null && latest.getSensor().getPrimaryZone() != null ? latest.getSensor().getPrimaryZone().getZoneId() : null;
        String sensorId = latest.getSensor() != null ? latest.getSensor().getSensorId() : null;

        return new TrendDto(sensorId, zoneId, latest.getId().getTimestamp(), v, pv, new TrendDelta(abs, pct, comparedTo));
    }
}
