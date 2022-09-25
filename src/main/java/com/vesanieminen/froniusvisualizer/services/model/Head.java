package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

public class Head implements Serializable {
    private RequestArguments requestArguments;
    private Status status;
    private ZonedDateTime timeStamp = null;

    public RequestArguments getRequestArguments() {
        return requestArguments;
    }

    public void setRequestArguments(RequestArguments requestArguments) {
        this.requestArguments = requestArguments;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(ZonedDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }
}
