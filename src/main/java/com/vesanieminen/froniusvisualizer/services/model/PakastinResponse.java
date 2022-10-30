package com.vesanieminen.froniusvisualizer.services.model;

import java.time.Instant;
import java.util.List;

public class PakastinResponse implements ResponseValidator {
    public List<Price> prices;

    public static class Price {
        public double value;
        public Instant date;
    }

    @Override
    public Object[] getObjects() {
        return new Object[]{this, prices};
    }

}
