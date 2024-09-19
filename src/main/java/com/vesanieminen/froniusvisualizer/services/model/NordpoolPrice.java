package com.vesanieminen.froniusvisualizer.services.model;

import lombok.Setter;

import java.time.Instant;

@Setter
public class NordpoolPrice {
    public long time;
    public double price;

    public NordpoolPrice(double price, long time) {
        this.time = time;
        this.price = price;
    }

    public long time() {
        return time;
    }

    public Instant timeInstant() {
        return Instant.ofEpochMilli(time);
    }

    public double price() {
        return price;
    }

}
