package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.util.Map;

public class Produced implements Serializable {
    private String unit = "";
    private Map<String, Double> values;

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Map<String, Double> getValues() {
        return values;
    }

    public void setValues(Map<String, Double> values) {
        this.values = values;
    }
}
