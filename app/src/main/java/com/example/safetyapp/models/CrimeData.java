package com.example.safetyapp.models;

public class CrimeData {
    private double latitude;
    private double longitude;
    private String type;
    private String description;
    private long timestamp;

    public CrimeData() {} // Required for Firebase

    public CrimeData(double latitude, double longitude, String type, String description, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
}
