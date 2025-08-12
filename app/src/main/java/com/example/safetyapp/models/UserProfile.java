package com.example.safetyapp.models;

public class UserProfile {
    private String name;
    private String phoneNumber;
    private String emergencyContact;

    public UserProfile() {}

    public UserProfile(String name, String phoneNumber, String emergencyContact) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.emergencyContact = emergencyContact;
    }

    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmergencyContact() { return emergencyContact; }
}
