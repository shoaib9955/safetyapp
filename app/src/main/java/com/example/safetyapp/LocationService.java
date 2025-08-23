package com.example.safetyapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "safetyAppChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ZoneNotifier zoneNotifier;
    private List<CrimeHeatmapActivity.CrimePoint> zones = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        zoneNotifier = new ZoneNotifier(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load saved zones
        String savedZonesJson = getSharedPreferences("safetyAppPrefs", MODE_PRIVATE)
                .getString("fixedZones", null);
        if (savedZonesJson != null) {
            zones.clear();
            zones.addAll(CrimeHeatmapActivity.CrimePoint.fromJsonArray(savedZonesJson));
        }

        createNotificationChannel();
        startForeground(1, getNotification());
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(5000);
        request.setFastestInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    zoneNotifier.checkZoneEntry(currentLatLng, zones);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safety App Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety App Active")
                .setContentText("Monitoring zones in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    @Override
    public void onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
