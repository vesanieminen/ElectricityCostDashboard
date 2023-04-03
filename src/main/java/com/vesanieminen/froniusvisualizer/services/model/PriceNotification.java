package com.vesanieminen.froniusvisualizer.services.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author mstahv
 */
@Getter
@Setter
public class PriceNotification {
    String uid;
    boolean enabled;
    double price = 20;
    boolean up = true;
    int prewarn = 0;
    int timeout = 0;
    String extraMsg = "";
    @JsonIgnore // too hard for Jackson ?
    Instant lastTriggered;
    
}
