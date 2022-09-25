package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

public class Inverter implements Serializable {
    private Data data;
    private int deviceType = 0;
    private ZonedDateTime end = null;
    private int nodeType = 0;
    private ZonedDateTime start = null;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public void setEnd(ZonedDateTime end) {
        this.end = end;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public void setStart(ZonedDateTime start) {
        this.start = start;
    }
}
