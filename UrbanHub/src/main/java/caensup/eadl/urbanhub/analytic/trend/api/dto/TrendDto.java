package caensup.eadl.urbanhub.analytic.trend.api.dto;

import java.time.OffsetDateTime;

public class TrendDto {

    private String sensorId;
    private String zoneId;
    private OffsetDateTime timestamp;
    private Float value;
    private Float previousValue;
    private Float changeAbsolute;
    private Float changePercent;
    private String comparedTo; // e.g. "N-1", "N-24h"

    public TrendDto() {
    }

    public TrendDto(
            String sensorId,
            String zoneId,
            OffsetDateTime timestamp,
            Float value,
            Float previousValue,
            TrendDelta delta) {
        this.sensorId = sensorId;
        this.zoneId = zoneId;
        this.timestamp = timestamp;
        this.value = value;
        this.previousValue = previousValue;
        this.changeAbsolute = delta.changeAbsolute();
        this.changePercent = delta.changePercent();
        this.comparedTo = delta.comparedTo();
    }

    // getters and setters

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public Float getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(Float previousValue) {
        this.previousValue = previousValue;
    }

    public Float getChangeAbsolute() {
        return changeAbsolute;
    }

    public void setChangeAbsolute(Float changeAbsolute) {
        this.changeAbsolute = changeAbsolute;
    }

    public Float getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Float changePercent) {
        this.changePercent = changePercent;
    }

    public String getComparedTo() {
        return comparedTo;
    }

    public void setComparedTo(String comparedTo) {
        this.comparedTo = comparedTo;
    }
}

