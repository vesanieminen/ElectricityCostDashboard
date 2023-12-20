package com.vesanieminen.froniusvisualizer.services.model;

import java.util.List;

public class TVOResponse {

    public String created;
    public List<Data> predictions;

    public static class Data {
        public String time;
        public int prediction;
    }

}
