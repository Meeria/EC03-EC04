package caensup.eadl.urbanhub.config;

import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.entity.MeasureId;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.SensorType;
import caensup.eadl.urbanhub.entity.Zone;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import caensup.eadl.urbanhub.repository.SensorRepository;
import caensup.eadl.urbanhub.repository.SensorTypeRepository;
import caensup.eadl.urbanhub.repository.ZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import caensup.eadl.urbanhub.ingest.service.MeasureIngestService;
import caensup.eadl.urbanhub.ingest.api.dto.IngestMeasureJson;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class DataSeeder {

    private static final String POLUTION_UNIT = "μg/m3";

    // 60 diverse locations from caen-locations.json spread across Caen
    private static final double[][] CAEN_LOCATIONS = {
            { 49.16212, -0.36901 },
            { 49.21231, -0.40660 },
            { 49.18285, -0.35354 },
            { 49.17124, -0.32896 },
            { 49.16036, -0.33659 },
            { 49.18268, -0.39533 },
            { 49.21483, -0.34574 },
            { 49.20914, -0.38129 },
            { 49.15521, -0.39919 },
            { 49.16875, -0.40940 },
            { 49.19527, -0.32717 },
            { 49.15523, -0.39728 },
            { 49.20820, -0.32001 },
            { 49.19989, -0.35203 },
            { 49.19217, -0.33222 },
            { 49.17506, -0.36256 },
            { 49.20835, -0.35521 },
            { 49.16019, -0.33452 },
            { 49.20401, -0.36943 },
            { 49.16201, -0.32830 },
            { 49.19536, -0.34356 },
            { 49.17892, -0.35629 },
            { 49.15793, -0.34869 },
            { 49.16300, -0.34766 },
            { 49.20561, -0.39217 },
            { 49.15922, -0.33438 },
            { 49.18779, -0.33052 },
            { 49.19874, -0.33063 },
            { 49.19606, -0.33724 },
            { 49.18402, -0.33161 },
            { 49.20555, -0.37748 },
            { 49.16579, -0.40159 },
            { 49.16830, -0.40601 },
            { 49.19824, -0.40693 },
            { 49.20244, -0.34792 },
            { 49.19896, -0.34296 },
            { 49.16872, -0.33300 },
            { 49.16378, -0.39377 },
            { 49.19441, -0.36370 },
            { 49.16047, -0.35066 },
            { 49.17061, -0.32129 },
            { 49.16795, -0.36263 },
            { 49.18665, -0.33790 },
            { 49.17491, -0.37403 },
            { 49.19828, -0.39805 },
            { 49.19908, -0.32235 },
            { 49.21157, -0.35810 },
            { 49.20698, -0.33189 },
            { 49.15670, -0.37961 },
            { 49.18801, -0.38996 },
            { 49.20700, -0.36586 },
            { 49.18919, -0.38873 },
            { 49.19826, -0.33011 },
            { 49.16549, -0.36594 },
            { 49.20545, -0.34138 },
            { 49.16430, -0.40045 },
            { 49.21082, -0.35696 },
            { 49.16869, -0.32400 },
            { 49.16846, -0.40255 },
            { 49.16916, -0.38202 },
    };

    private static final String UNIT_AIR = "μg/m3";
    private static final String UNIT_NOISE = "dB";
    private static final String UNIT_TRAFFIC = "km/h";
    private static final String UNIT_WEATHER = "°C";

    private String coords(double lat, double lon) {
        return lat + "," + lon;
    }

    @Bean
    public CommandLineRunner seedData(ZoneRepository zoneRepository,
                                      SensorTypeRepository sensorTypeRepository,
                                      SensorRepository sensorRepository,
                                      MeasureRepository measureRepository,
                                      MeasureIngestService ingestService) {
        return args -> {
            if (!zoneRepository.findAll().isEmpty()) {
                // don't seed if DB not empty
                return;
            }

            // create sensor types
            SensorType air = new SensorType();
            air.setSensorTypeId("AIR");
            SensorType noise = new SensorType();
            noise.setSensorTypeId("NOISE");
            sensorTypeRepository.saveAll(List.of(air, noise));

            // create zones
            Zone z1 = new Zone(); z1.setZoneId("CENTRE");
            Zone z2 = new Zone(); z2.setZoneId("NORTH");
            zoneRepository.saveAll(List.of(z1, z2));

            // create sensors
            Sensor s1 = new Sensor(); s1.setSensorId("sensor-centre-1"); s1.setLatitude(45.0); s1.setLongitude(3.0); s1.setZones(Set.of(z1)); s1.setSensorType(air);
            Sensor s2 = new Sensor(); s2.setSensorId("sensor-centre-2"); s2.setLatitude(45.1); s2.setLongitude(3.1); s2.setZones(Set.of(z1)); s2.setSensorType(noise);
            Sensor s3 = new Sensor(); s3.setSensorId("sensor-north-1"); s3.setLatitude(45.5); s3.setLongitude(3.5); s3.setZones(Set.of(z2)); s3.setSensorType(air);
            // 4 sensor types × 15 locations = 60 sensors
            String[] types = { "AIR", "NOISE", "TRAFFIC", "WEATHER" };
            String[] units = { UNIT_AIR, UNIT_NOISE, UNIT_TRAFFIC, UNIT_WEATHER };
            double[][] ranges = {
                    { 30.0, 55.0 }, // AIR
                    { 55.0, 80.0 }, // NOISE
                    { 40.0, 140.0 }, // TRAFFIC
                    { 13.0, 22.0 }, // WEATHER
            };

            sensorRepository.saveAll(List.of(s1, s2, s3));
            int numLocations = 15;

            // Fixed sample rows (several timestamps including ~24h apart) — once, not per ingest iteration
            OffsetDateTime now = OffsetDateTime.now();
            Measure m1 = new Measure();
            m1.setId(new MeasureId(now.minusHours(1), UUID.randomUUID()));
            m1.setValue(10.0f); m1.setUnit(POLUTION_UNIT); m1.setSensor(s1);
            Measure m2 = new Measure();
            m2.setId(new MeasureId(now.minusHours(2), UUID.randomUUID()));
            m2.setValue(8.0f); m2.setUnit(POLUTION_UNIT); m2.setSensor(s1);
            Measure m3 = new Measure();
            m3.setId(new MeasureId(now.minusHours(24).plusMinutes(10), UUID.randomUUID()));
            m3.setValue(6.0f); m3.setUnit(POLUTION_UNIT); m3.setSensor(s1);
            Measure m4 = new Measure();
            m4.setId(new MeasureId(now.minusHours(1), UUID.randomUUID()));
            m4.setValue(70.0f); m4.setUnit("dB"); m4.setSensor(s2);
            Measure m5 = new Measure();
            m5.setId(new MeasureId(now.minusHours(25), UUID.randomUUID()));
            m5.setValue(65.0f); m5.setUnit("dB"); m5.setSensor(s2);
            Measure m6 = new Measure();
            m6.setId(new MeasureId(now.minusHours(1), UUID.randomUUID()));
            m6.setValue(12.0f); m6.setUnit(POLUTION_UNIT); m6.setSensor(s3);
            measureRepository.saveAll(List.of(m1, m2, m3, m4, m5, m6));

            // 48h of data, 1 measurement every 30 min per synthetic sensor id
            for (int t = 0; t < types.length; t++) {
                double[] range = ranges[t];
                for (int i = 0; i < numLocations; i++) {
                    String sensorId = types[t] + "_" + (i + 1);
                    double jitterLat = (Math.random() - 0.5) * 0.0006;
                    double jitterLon = (Math.random() - 0.5) * 0.0006;
                    double lat = CAEN_LOCATIONS[i][0] + jitterLat;
                    double lon = CAEN_LOCATIONS[i][1] + jitterLon;
                    for (int step = 48 * 2; step >= 0; step--) {
                        Instant ts = Instant.now().minus((long) step * 30, ChronoUnit.MINUTES);
                        double hourOfDay = (ts.getEpochSecond() / 3600.0) % 24;
                        double dailyPattern = Math.sin(2 * Math.PI * (hourOfDay - 6) / 24) * 0.3 + 0.7;
                        double value = range[0] + Math.random() * (range[1] - range[0]) * dailyPattern;
                        value = Math.round(value * 10.0) / 10.0;
                        ingestService.ingestMeasure(new IngestMeasureJson(
                                sensorId,
                                types[t],
                                String.valueOf(ts.toEpochMilli()),
                                coords(lat, lon),
                                value,
                                units[t]));
                    }
                }
            }

        };
    }
}