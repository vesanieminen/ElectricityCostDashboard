package com.vesanieminen.froniusvisualizer.services.model;

import java.time.ZonedDateTime;

public class SpotHintaResponse implements ResponseValidator {
    public ZonedDateTime TimeStamp;
    public double Temperature;

    @Override
    public Object[] getObjects() {
        return new Object[]{this, TimeStamp, Temperature};
    }

}
