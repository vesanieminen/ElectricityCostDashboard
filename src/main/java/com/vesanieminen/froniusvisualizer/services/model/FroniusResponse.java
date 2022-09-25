package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class FroniusResponse implements Serializable {
    public Head head;
    public Body body;

    public static class Head implements Serializable {
        public RequestArguments requestArguments;
        public Status status;
        public ZonedDateTime timeStamp;
    }

    public static class RequestArguments implements Serializable {
        public List<String> channel;
        public ZonedDateTime endDate;
        public String humanReadable;
        public String scope;
        public String seriesType;
        public ZonedDateTime startDate;
    }

    public static class Status implements Serializable {
        public int code;
        public String reason;
        public String userMessage;
    }

    public static class Body implements Serializable {
        public Map<String, Inverter> data;
    }

    public static class Inverter implements Serializable {
        public Data data;
        public int deviceType;
        public ZonedDateTime end;
        public int nodeType;
        public ZonedDateTime start;
    }

    public static class Data implements Serializable {
        public Produced energyReal_WAC_Sum_Produced;
    }

    public static class Produced implements Serializable {
        public String unit;
        public Map<String, Double> values;
    }

}
