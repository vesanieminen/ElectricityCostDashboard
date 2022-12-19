package com.vesanieminen.froniusvisualizer.services.model;

import java.time.Instant;

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

    public void setTime(long time) {
        this.time = time;
    }

    public Instant timeInstant() {
        return Instant.ofEpochMilli(time);
    }

    public double price() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

}
