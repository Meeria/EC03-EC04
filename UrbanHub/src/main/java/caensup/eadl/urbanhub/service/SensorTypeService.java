package caensup.eadl.urbanhub.service;

import caensup.eadl.urbanhub.dto.SensorTypeDto;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.ingest.exception.SensorTypeNotFoundException;
import caensup.eadl.urbanhub.repository.SensorTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SensorTypeService {

    private final SensorTypeRepository sensorTypeRepository;

    public SensorTypeService(SensorTypeRepository sensorTypeRepository) {
        this.sensorTypeRepository = sensorTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<SensorTypeDto> getAll() {
        return sensorTypeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorTypeDto getById(String sensorTypeId) {
        return sensorTypeRepository.findBySensorTypeId(sensorTypeId)
                .map(this::toDto)
                .orElseThrow(() -> new SensorTypeNotFoundException(sensorTypeId));
    }

    @Transactional(readOnly = true)
    public long getCount() {
        return sensorTypeRepository.count();
    }

    private SensorTypeDto toDto(SensorType sensorType) {
        return new SensorTypeDto(sensorType.getUuid(), sensorType.getSensorTypeId());
    }
}
