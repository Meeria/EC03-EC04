package caensup.eadl.urbanhub.controller;

import caensup.eadl.urbanhub.dto.CreateZoneDto;
import caensup.eadl.urbanhub.dto.UpdateZoneDto;
import caensup.eadl.urbanhub.dto.ZoneDto;
import caensup.eadl.urbanhub.service.ZoneService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    public List<ZoneDto> getAll() {
        return zoneService.getAll();
    }

    @GetMapping("/count")
    public long getCount() {
        return zoneService.getCount();
    }

    @GetMapping("/by-id")
    public ZoneDto getById(@RequestParam(name = "zone_id") String zoneId) {
        return zoneService.getById(zoneId);
    }

    @PostMapping
    public ZoneDto create(@Valid @RequestBody CreateZoneDto dto) {
        return zoneService.create(dto);
    }

    @PutMapping("/{zoneId}")
    public ZoneDto update(@PathVariable String zoneId, @Valid @RequestBody UpdateZoneDto dto) {
        return zoneService.update(zoneId, dto);
    }

    @DeleteMapping("/{zoneId}")
    public void delete(@PathVariable String zoneId) {
        zoneService.delete(zoneId);
    }
}
