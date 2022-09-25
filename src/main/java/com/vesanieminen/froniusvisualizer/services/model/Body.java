package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.util.Map;

public class Body implements Serializable {
    private Map<String, Inverter> data;

    public Map<String, Inverter> getData() {
        return data;
    }

    public void setData(Map<String, Inverter> data) {
        this.data = data;
    }
}
