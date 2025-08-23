package com.example.safetyapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ZoneNotifier {

    private final Context context;
    private final Set<CrimeHeatmapActivity.CrimePoint> notifiedZones = new HashSet<>();
    private TextToSpeech tts; // ← not final

    public ZoneNotifier(Context context) {
        this.context = context;

        // Initialize TTS safely
        tts = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR && tts != null) {
                tts.setLanguage(Locale.US);
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
            }
        });
    }

    public void checkZoneEntry(LatLng userLocation, List<CrimeHeatmapActivity.CrimePoint> zones) {
        boolean smsEnabled = context.getSharedPreferences("safetyAppPrefs", Context.MODE_PRIVATE)
                .getBoolean("smsEnabled", true);
        boolean voiceEnabled = context.getSharedPreferences("safetyAppPrefs", Context.MODE_PRIVATE)
                .getBoolean("voiceEnabled", true);

        for (CrimeHeatmapActivity.CrimePoint zone : zones) {
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    zone.location.latitude, zone.location.longitude,
                    distance
            );

            float zoneRadius = 300f;
            float preEntryRadius = 30f;

            if (distance[0] <= zoneRadius) {
                if (!notifiedZones.contains(zone)) {
                    if (smsEnabled) sendSms(zone.intensity, true);
                    if (voiceEnabled) speak(zone.intensity, true);
                    notifiedZones.add(zone);
                }
            } else if (distance[0] <= zoneRadius + preEntryRadius) {
                if (smsEnabled) sendSms(zone.intensity, false);
                if (voiceEnabled) speak(zone.intensity, false);
            } else {
                notifiedZones.remove(zone);
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

    private void speak(double intensity, boolean inside) {
        if (tts == null) return; // safety check

        String message;
        if (intensity < 0.3) message = inside ? "You entered a safe zone" : "You are approaching a safe zone";
        else if (intensity < 0.8) message = inside ? "You entered a moderate zone" : "You are approaching a moderate zone";
        else message = inside ? "Danger! You entered a high-risk zone!" : "Danger! You are approaching a high-risk zone!";

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
