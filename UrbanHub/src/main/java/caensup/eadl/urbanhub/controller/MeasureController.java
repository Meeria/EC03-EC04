package caensup.eadl.urbanhub.controller;

import caensup.eadl.urbanhub.dto.MeasureDto;
import caensup.eadl.urbanhub.service.MeasureQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes endpoints for measure consultation.
 */
@RestController
@RequestMapping("/api/measures")
public class MeasureController {

    private final MeasureQueryService measureQueryService;

    public MeasureController(MeasureQueryService measureQueryService) {
        this.measureQueryService = measureQueryService;
    }


    /**
     * Returns the list of measures, with optional filtering by sensor functional identifier.
     */
    @GetMapping
    public List<MeasureDto> getMeasures(@RequestParam(name = "sensor_id", required = false) String sensorId) {
        return measureQueryService.getMeasures(sensorId);
    }

    @GetMapping("/count")
    public long getCount() {
        return measureQueryService.getCount();
    }

    @GetMapping("/by-day")
    public List<MeasureDto> getMeasuresByDay(@RequestParam String date) {
        return measureQueryService.getMeasuresByDay(date);
    }

    @GetMapping("/by-date-range")
    public List<MeasureDto> getMeasuresBetween(
            @RequestParam String from,
            @RequestParam String to) {
        return measureQueryService.getMeasuresBetween(from, to);
    }
}
