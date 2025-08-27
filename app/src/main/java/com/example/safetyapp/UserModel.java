// File: app/src/main/java/com/example/safetyapp/UserModel.java
package com.example.safetyapp;

public class UserModel {
    private String name;
    private String email;

    // Constructor
    public UserModel(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Empty constructor (required by Firebase)
    public UserModel() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
