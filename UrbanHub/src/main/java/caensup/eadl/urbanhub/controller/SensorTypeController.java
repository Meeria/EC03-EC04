package caensup.eadl.urbanhub.controller;

import caensup.eadl.urbanhub.dto.SensorTypeDto;
import caensup.eadl.urbanhub.service.SensorTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-types")
public class SensorTypeController {

    private final SensorTypeService sensorTypeService;

    public SensorTypeController(SensorTypeService sensorTypeService) {
        this.sensorTypeService = sensorTypeService;
    }

    @GetMapping
    public List<SensorTypeDto> getAll() {
        return sensorTypeService.getAll();
    }

    @GetMapping("/count")
    public long getCount() {
        return sensorTypeService.getCount();
    }

    @GetMapping("/by-id")
    public SensorTypeDto getById(@RequestParam(name = "sensor_type_id") String sensorTypeId) {
        return sensorTypeService.getById(sensorTypeId);
    }

}
