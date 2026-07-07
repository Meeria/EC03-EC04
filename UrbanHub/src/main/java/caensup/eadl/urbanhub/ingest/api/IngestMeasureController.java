package caensup.eadl.urbanhub.ingest.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import caensup.eadl.urbanhub.ingest.service.MeasureIngestService;

import jakarta.validation.Valid;

@RestController
public class IngestMeasureController {

    private final MeasureIngestService measureIngestService;

    public IngestMeasureController(MeasureIngestService measureIngestService) {
        this.measureIngestService = measureIngestService;
    }

    @PostMapping("/ingest/measures")
    public void ingestMeasure(@Valid @RequestBody IngestMeasureJson json) {
        measureIngestService.ingestMeasure(json);
    }
}
