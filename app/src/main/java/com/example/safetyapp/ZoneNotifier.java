package com.example.safetyapp;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ZoneNotifier {

    private final Context context;
    private final Set<CrimeHeatmapActivity.CrimePoint> notifiedZones = new HashSet<>();

    public ZoneNotifier(Context context) {
        this.context = context;
    }

    public void checkZoneEntry(LatLng userLocation, List<CrimeHeatmapActivity.CrimePoint> zones) {
        for (CrimeHeatmapActivity.CrimePoint zone : zones) {
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    zone.location.latitude, zone.location.longitude,
                    distance
            );

            float zoneRadius = 300f;        // zone radius
            float preEntryRadius = 30f;     // pre-entry radius

            if (distance[0] <= zoneRadius) {
                // Inside zone
                if (!notifiedZones.contains(zone)) {
                    sendSms(zone.intensity, true);
                    notifiedZones.add(zone);
                }
            } else if (distance[0] <= zoneRadius + preEntryRadius) {
                // Pre-entry zone alert
                sendSms(zone.intensity, false);
            } else {
                notifiedZones.remove(zone); // reset for next entry
            }
        }
    }

    private void sendSms(double intensity, boolean inside) {
        String number = context.getSharedPreferences("safetyAppPrefs", Context.MODE_PRIVATE)
                .getString("emergencyNumber", "");

        if (number == null || number.isEmpty()) return;

        String message;
        if (intensity < 0.3) message = inside ? "You entered a safe zone" : "You are approaching a safe zone";
        else if (intensity < 0.8) message = inside ? "You entered a moderate zone" : "You are approaching a moderate zone";
        else message = inside ? "Danger! You entered a high-risk zone!" : "Danger! You are approaching a high-risk zone!";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
