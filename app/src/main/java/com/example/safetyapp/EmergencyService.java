package com.example.safetyapp;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.safetyapp.utils.SmsHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class EmergencyService extends Service {

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // ✅ Get saved emergency number
        SharedPreferences prefs = getSharedPreferences("SafetyAppPrefs", MODE_PRIVATE);
        String emergencyNumber = prefs.getString("emergency_number", "1234567890");

        sendEmergencySMS(emergencyNumber);

        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendEmergencySMS(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            SmsHelper.sendSMS(this, number, "🚨 EMERGENCY! I need help immediately.");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String message = "🚨 EMERGENCY! I need help.\nLocation: " +
                        "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                SmsHelper.sendSMS(this, number, message);
            } else {
                SmsHelper.sendSMS(this, number, "🚨 EMERGENCY! I need help. Location unavailable.");
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
