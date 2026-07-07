package caensup.eadl.urbanhub.service;

import caensup.eadl.urbanhub.dto.SensorDto;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.ingest.exception.SensorNotFoundException;
import caensup.eadl.urbanhub.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SensorService {

    private static final Duration SENSOR_ALIVE_WINDOW = Duration.ofHours(1);

    private final SensorRepository sensorRepository;

    public SensorService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Transactional(readOnly = true)
    public List<SensorDto> getAll() {
        Instant cutoff = Instant.now().minus(SENSOR_ALIVE_WINDOW);
        return sensorRepository.findAll().stream()
                .map(s -> toDto(s, cutoff))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorDto> getByType(String sensorTypeId) {
        Instant cutoff = Instant.now().minus(SENSOR_ALIVE_WINDOW);
        return sensorRepository.findBySensorType_SensorTypeId(sensorTypeId).stream()
                .map(s -> toDto(s, cutoff))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorDto> getByStatus(boolean alive) {
        Instant cutoff = Instant.now().minus(SENSOR_ALIVE_WINDOW);
        List<Sensor> sensors = alive
                ? sensorRepository.findByLastUpdateGreaterThanEqual(cutoff)
                : sensorRepository.findByLastUpdateLessThan(cutoff);
        return sensors.stream().map(s -> toDto(s, cutoff)).toList();
    }

    @Transactional(readOnly = true)
    public long getByStatusCount(boolean alive) {
        Instant cutoff = Instant.now().minus(SENSOR_ALIVE_WINDOW);
        List<Sensor> sensors = alive
                ? sensorRepository.findByLastUpdateGreaterThanEqual(cutoff)
                : sensorRepository.findByLastUpdateLessThan(cutoff);
        return sensors.size();
    }

    @Transactional(readOnly = true)
    public long getCount() {
        return sensorRepository.count();
    }

    @Transactional(readOnly = true)
    public SensorDto getById(String sensorId) {
        Instant cutoff = Instant.now().minus(SENSOR_ALIVE_WINDOW);
        return sensorRepository.findBySensorId(sensorId)
                .map(s -> toDto(s, cutoff))
                .orElseThrow(() -> new SensorNotFoundException(sensorId));
    }

    private SensorDto toDto(Sensor s, Instant cutoff) {
        String zoneId = s.getZones() == null ? null
                : s.getZones().stream()
                        .findFirst()
                        .map(z -> z.getZoneId())
                        .orElse(null);
        return new SensorDto(
                s.getUuid(),
                s.getSensorId(),
                s.getSensorType().getSensorTypeId(),
                s.getLatitude(),
                s.getLongitude(),
                s.getLastUpdate() != null && !s.getLastUpdate().isBefore(cutoff),
                zoneId);
    }
}
