package caensup.eadl.urbanhub.analytic.average.service;

import caensup.eadl.urbanhub.analytic.average.model.KPI;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import caensup.eadl.urbanhub.types.measures.MeasureType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CalculationKPIService {


    private final MeasureRepository measureRepository;

    public CalculationKPIService(MeasureRepository measureRepository) {
        this.measureRepository = measureRepository;
    }

    public List<KPI> getKPIbyType(MeasureType measureType, String bucket, Instant start, Instant end) {
        return measureRepository.getAverageBySensorTypeId(measureType.name(), bucket, start, end).stream().map(f -> new KPI(f.getBucket(), f.getAverage(), f.getUnit())).toList();

    }

    public Map<String, List<KPI>> getKPIbyZone(String zoneId, String bucket, Instant start, Instant end) {
        Map<String, List<KPI>> mapKPIsOfType = new HashMap<>();
        for (MeasureType measureType : MeasureType.values()) {
            mapKPIsOfType.put(measureType.name(), measureRepository.getAverageByZoneId(zoneId, bucket, measureType.name(), start, end).stream().map(f -> new KPI(f.getBucket(), f.getAverage(), f.getUnit())).toList());
        }
        return mapKPIsOfType;
    }

    public List<KPI> getKPIbySensorId(String sensorId, String bucket, Instant start, Instant end) {
        return measureRepository.getAverageBySensorId(sensorId, bucket, start, end).stream().map(f -> new KPI(f.getBucket(), f.getAverage(), f.getUnit())).toList();
    }
}
