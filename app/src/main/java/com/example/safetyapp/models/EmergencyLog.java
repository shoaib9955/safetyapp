package com.example.safetyapp.models;

public class EmergencyLog {
    private String dateTime;
    private String location;
    private String triggeredBy;

    public EmergencyLog() {}

    public EmergencyLog(String dateTime, String location, String triggeredBy) {
        this.dateTime = dateTime;
        this.location = location;
        this.triggeredBy = triggeredBy;
    }

    public String getDateTime() { return dateTime; }
    public String getLocation() { return location; }
    public String getTriggeredBy() { return triggeredBy; }
}
