package caensup.eadl.urbanhub.service;

import caensup.eadl.urbanhub.dto.MeasureDto;
import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Logique de consultation des mesures (lecture seule).
 * Distinct de {@link caensup.eadl.urbanhub.ingest.service.MeasureIngestService} qui gère l'écriture.
 */
@Service
public class MeasureQueryService {

    private final MeasureRepository measureRepository;

    public MeasureQueryService(MeasureRepository measureRepository) {
        this.measureRepository = measureRepository;
    }

    @Transactional(readOnly = true)
    public long getCount() {
        return measureRepository.count();
    }

    @Transactional(readOnly = true)
    public List<MeasureDto> getMeasuresByDay(String date) {
        try {
            OffsetDateTime from = LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime to = LocalDate.parse(date).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
            return measureRepository.findBetween(from, to).stream()
                    .map(this::toDto)
                    .toList();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected yyyy-MM-dd");
        }
    }

    @Transactional(readOnly = true)
    public List<MeasureDto> getMeasuresBetween(String fromDate, String toDate) {
        try {
            OffsetDateTime from = LocalDate.parse(fromDate).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime to = LocalDate.parse(toDate).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
            return measureRepository.findBetween(from, to).stream()
                    .map(this::toDto)
                    .toList();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected yyyy-MM-dd");
        }
    }

    @Transactional(readOnly = true)
    public List<MeasureDto> getMeasures(String sensorId) {
        List<Measure> measures = sensorId == null || sensorId.isBlank()
                ? measureRepository.findAll()
                : measureRepository.findBySensor_SensorId(sensorId);

        return measures.stream()
                .map(this::toDto)
                .toList();
    }

    private MeasureDto toDto(Measure measure) {
        Sensor sensor = measure.getSensor();
        String zoneIds = sensor.getZones() == null || sensor.getZones().isEmpty()
                ? null
                : sensor.getZones().stream()
                        .map(z -> z.getZoneId())
                        .collect(Collectors.joining(","));

        return new MeasureDto(
                measure.getId().getTimestamp() != null ? UUID.randomUUID() : null,
                null,
                measure.getId().getTimestamp(),
                measure.getValue(),
                measure.getUnit(),
                sensor.getSensorId(),
                sensor.getLatitude(),
                sensor.getLongitude(),
                zoneIds,
                sensor.getSensorType().getSensorTypeId()
        );
    }
}
