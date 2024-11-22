package org.example;

public class RequestRecord {
    private long startTime;
    private String requestType;
    private long latency;
    private int responseCode;

    public RequestRecord(long startTime, String requestType, long latency, int responseCode) {
        this.startTime = startTime;
        this.requestType = requestType;
        this.latency = latency;
        this.responseCode = responseCode;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getRequestType() {
        return requestType;
    }

    public long getLatency() {
        return latency;
    }

    public int getResponseCode() {
        return responseCode;
    }
}

