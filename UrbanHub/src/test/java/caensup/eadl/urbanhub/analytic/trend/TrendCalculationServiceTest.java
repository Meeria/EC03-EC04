package caensup.eadl.urbanhub.analytic.trend;

import caensup.eadl.urbanhub.analytic.trend.api.dto.TrendDto;
import caensup.eadl.urbanhub.analytic.trend.api.service.TrendCalculationService;
import caensup.eadl.urbanhub.entity.Measure;
import caensup.eadl.urbanhub.entity.MeasureId;
import caensup.eadl.urbanhub.entity.Sensor;
import caensup.eadl.urbanhub.entity.Zone;
import caensup.eadl.urbanhub.repository.MeasureRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TrendCalculationServiceTest {

    private MeasureRepository repo;
    private TrendCalculationService service;

    @BeforeEach
    public void setUp() {
        repo = Mockito.mock(MeasureRepository.class);
        service = new TrendCalculationService(repo);
    }

    @Test
    public void testLatestVsPrevious() {
        Sensor s = new Sensor();
        s.setSensorId("S1");
        Zone z = new Zone(); z.setZoneId("Z1"); s.getZones().add(z);

        Measure m1 = new Measure();
        m1.setId(new MeasureId(OffsetDateTime.parse("2026-04-15T10:00:00Z"), UUID.randomUUID()));
        m1.setValue(20.0f);
        m1.setSensor(s);

        Measure m2 = new Measure();
        m2.setId(new MeasureId(OffsetDateTime.parse("2026-04-15T09:00:00Z"), UUID.randomUUID()));
        m2.setValue(10.0f);
        m2.setSensor(s);

        when(repo.findTop2BySensor_SensorIdOrderById_TimestampDesc("S1")).thenReturn(List.of(m1, m2));

        Optional<TrendDto> res = service.computeTrendLatestVsPrevious("S1");
        assertTrue(res.isPresent());
        TrendDto t = res.get();
        assertEquals("S1", t.getSensorId());
        assertEquals(20.0f, t.getValue());
        assertEquals(10.0f, t.getPreviousValue());
        assertEquals(10.0f, t.getChangeAbsolute());
        assertEquals(100.0f, t.getChangePercent());
        assertEquals("N-1", t.getComparedTo());
    }

    @Test
    public void testLatestVs24h_chooseNearest() {
        Sensor s = new Sensor();
        s.setSensorId("S2");
        Zone z = new Zone(); z.setZoneId("Z2"); s.getZones().add(z);

        OffsetDateTime now = OffsetDateTime.parse("2026-04-15T12:00:00Z");

        Measure latest = new Measure();
        latest.setId(new MeasureId(now, UUID.randomUUID()));
        latest.setValue(50.0f);
        latest.setSensor(s);

        Measure before = new Measure();
        before.setId(new MeasureId(now.minusHours(24).minusMinutes(30), UUID.randomUUID()));
        before.setValue(40.0f);
        before.setSensor(s);

        Measure after = new Measure();
        after.setId(new MeasureId(now.minusHours(24).plusMinutes(20), UUID.randomUUID()));
        after.setValue(30.0f);
        after.setSensor(s);

        when(repo.findTopBySensor_SensorIdOrderById_TimestampDesc("S2")).thenReturn(Optional.of(latest));
        when(repo.findTopBySensor_SensorIdAndId_TimestampLessThanEqualOrderById_TimestampDesc(eq("S2"), any())).thenReturn(Optional.of(before));
        when(repo.findTopBySensor_SensorIdAndId_TimestampGreaterThanEqualOrderById_TimestampAsc(eq("S2"), any())).thenReturn(Optional.of(after));

        Optional<TrendDto> res = service.computeTrendLatestVs24h("S2");
        assertTrue(res.isPresent());
        TrendDto t = res.get();
        // nearest to -24h is 'after' (20 minutes) vs before(30 minutes) => choose after with value 30 => change 20
        assertEquals(50.0f, t.getValue());
        assertEquals(30.0f, t.getPreviousValue());
        assertEquals(20.0f, t.getChangeAbsolute());
        assertEquals((20.0f/30.0f)*100.0f, t.getChangePercent());
        assertEquals("N-24h", t.getComparedTo());
    }

    @Test
    public void testZonePeriod() {
        Sensor s1 = new Sensor(); s1.setSensorId("A1"); Zone z = new Zone(); z.setZoneId("ZP"); s1.getZones().add(z);
        Sensor s2 = new Sensor(); s2.setSensorId("A2"); s2.getZones().add(z);

        Measure m1 = new Measure();
        m1.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T10:00:00Z"), UUID.randomUUID()));
        m1.setValue(1.0f); m1.setSensor(s1);
        Measure m2 = new Measure();
        m2.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T11:00:00Z"), UUID.randomUUID()));
        m2.setValue(2.0f); m2.setSensor(s1);

        Measure m3 = new Measure();
        m3.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T09:30:00Z"), UUID.randomUUID()));
        m3.setValue(5.0f); m3.setSensor(s2);
        Measure m4 = new Measure();
        m4.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T12:00:00Z"), UUID.randomUUID()));
        m4.setValue(6.0f); m4.setSensor(s2);

        when(repo.findAll()).thenReturn(List.of(m1, m2, m3, m4));

        List<TrendDto> res = service.computeTrendsByZoneInPeriod("ZP", OffsetDateTime.parse("2026-04-10T09:00:00Z"), OffsetDateTime.parse("2026-04-10T12:00:00Z"));
        // we expect one trend per sensor: A1 and A2
        assertEquals(2, res.size());
    }

    @Test
    public void testSensorInPeriod() {
        Sensor s = new Sensor(); s.setSensorId("SP1"); Zone z = new Zone(); z.setZoneId("Z1"); s.getZones().add(z);

        Measure m1 = new Measure(); m1.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T10:00:00Z"), UUID.randomUUID())); m1.setValue(5.0f); m1.setSensor(s);
        Measure m2 = new Measure(); m2.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T11:00:00Z"), UUID.randomUUID())); m2.setValue(7.0f); m2.setSensor(s);
        Measure m3 = new Measure(); m3.setId(new MeasureId(OffsetDateTime.parse("2026-04-10T12:30:00Z"), UUID.randomUUID())); m3.setValue(9.0f); m3.setSensor(s);

        when(repo.findBySensor_SensorId("SP1")).thenReturn(List.of(m1, m2, m3));

        Optional<TrendDto> res = service.computeTrendForSensorInPeriod("SP1", OffsetDateTime.parse("2026-04-10T09:00:00Z"), OffsetDateTime.parse("2026-04-10T12:00:00Z"));
        assertTrue(res.isPresent());
        TrendDto t = res.get();
        // latest in window is m2 (11:00), prev is m1 (10:00)
        assertEquals(7.0f, t.getValue());
        assertEquals(5.0f, t.getPreviousValue());
        assertEquals(2.0f, t.getChangeAbsolute());
    }

    @Test
    public void testAllSensorsInPeriod() {
        Sensor s1 = new Sensor(); s1.setSensorId("B1"); Zone z = new Zone(); z.setZoneId("ZB"); s1.getZones().add(z);
        Sensor s2 = new Sensor(); s2.setSensorId("B2"); s2.getZones().add(z);

        Measure a1 = new Measure(); a1.setId(new MeasureId(OffsetDateTime.parse("2026-04-11T09:00:00Z"), UUID.randomUUID())); a1.setValue(1.0f); a1.setSensor(s1);
        Measure a2 = new Measure(); a2.setId(new MeasureId(OffsetDateTime.parse("2026-04-11T10:00:00Z"), UUID.randomUUID())); a2.setValue(2.0f); a2.setSensor(s1);

        Measure b1 = new Measure(); b1.setId(new MeasureId(OffsetDateTime.parse("2026-04-11T09:30:00Z"), UUID.randomUUID())); b1.setValue(3.0f); b1.setSensor(s2);
        Measure b2 = new Measure(); b2.setId(new MeasureId(OffsetDateTime.parse("2026-04-11T11:00:00Z"), UUID.randomUUID())); b2.setValue(4.0f); b2.setSensor(s2);

        when(repo.findAll()).thenReturn(List.of(a1,a2,b1,b2));

        List<TrendDto> res = service.computeTrendsInPeriod(OffsetDateTime.parse("2026-04-11T08:00:00Z"), OffsetDateTime.parse("2026-04-11T12:00:00Z"));
        assertEquals(2, res.size());
    }
}
