package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class FroniusResponse implements Serializable {
    public Head Head;
    public Body Body;

    public static class Head implements Serializable {
        public RequestArguments RequestArguments;
        public Status Status;
        public ZonedDateTime Timestamp;
    }

    public static class RequestArguments implements Serializable {
        public List<String> Channel;
        public ZonedDateTime EndDate;
        public String HumanReadable;
        public String Scope;
        public String SeriesType;
        public ZonedDateTime StartDate;
    }

    public static class Status implements Serializable {
        public int Code;
        public String Reason;
        public String UserMessage;
    }

    public static class Body implements Serializable {
        public Map<String, Inverter> Data;
    }

    public static class Inverter implements Serializable {
        public Data Data;
        public int DeviceType;
        public ZonedDateTime End;
        public int NodeType;
        public ZonedDateTime Start;
    }

    public static class Data implements Serializable {
        public Produced EnergyReal_WAC_Sum_Produced;
    }

    public static class Produced implements Serializable {
        public String Unit;
        public Map<String, Double> Values;
    }

}
