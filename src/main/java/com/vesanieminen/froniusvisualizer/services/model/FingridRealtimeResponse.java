package com.vesanieminen.froniusvisualizer.services.model;

import java.time.ZonedDateTime;
import java.util.List;

public class FingridRealtimeResponse implements ResponseValidator {

    public List<Data> HydroPower;
    public List<Data> NuclearPower;
    public List<Data> WindPower;
    public List<Data> SolarPower;
    public List<Data> Consumption;
    public List<Data> NetImportExport;

    public static class Data {
        public double value;
        public ZonedDateTime start_time;
        public ZonedDateTime end_time;
    }

    @Override
    public Object[] getObjects() {
        return new Object[]{this, HydroPower, NuclearPower, WindPower, SolarPower, Consumption, NetImportExport};
    }

}
