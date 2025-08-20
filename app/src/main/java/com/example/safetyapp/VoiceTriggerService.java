package com.example.safetyapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceTriggerService extends Service {

    private static final String TAG = "VoiceTriggerService";
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_trigger_channel";

    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private String currentLocationUrl = "Location not available";
    private boolean isLocationReady = false;

    // Preference keys
    private static final String PREFS_NAME = "SafetyAppPrefs";
    private static final String KEY_AUTO_CALL = "auto_call";
    private static final String KEY_SEND_SMS = "send_sms";
    private static final String KEY_USE_SPEAKER = "use_speaker";

    private static final String KEY_NUMBER_1 = "number1";
    private static final String KEY_NUMBER_2 = "number2";
    private static final String KEY_NUMBER_3 = "number3";
    private static final String KEY_NUMBER_1_ENABLED = "number1_enabled";
    private static final String KEY_NUMBER_2_ENABLED = "number2_enabled";
    private static final String KEY_NUMBER_3_ENABLED = "number3_enabled";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Keep CPU awake while listening
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafetyApp::VoiceWakeLock");
        wakeLock.acquire();

        setupSpeechRecognizer();
        startListening();
        startLocationUpdates();
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Log.e(TAG, "SpeechRecognizer error: " + error);
                new Handler(Looper.getMainLooper()).postDelayed(VoiceTriggerService.this::restartListening, 200);
            }

            @Override
            public void onResults(Bundle results) {
                processResults(results);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                processResults(partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
    }

    private void processResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            for (String result : matches) {
                if (result.toLowerCase(Locale.ROOT).contains("help")) {
                    Log.d(TAG, "Help detected!");
                    triggerEmergencyAction();
                    if (speechRecognizer != null) speechRecognizer.cancel();
                    break;
                }
            }
        }
        restartListening();
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void restartListening() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    // ---------------- Efficient Location Updates ----------------

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted!");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // update every 5 seconds
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    updateLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocation(Location location) {
        // Google Maps "navigate to" link
        currentLocationUrl = "https://www.google.com/maps/dir/?api=1&destination="
                + location.getLatitude() + "," + location.getLongitude();
        isLocationReady = true;
        Log.d(TAG, "Updated location (directions link): " + currentLocationUrl);
    }

    // ---------------- Emergency Actions ----------------

    private void triggerEmergencyAction() {
        if (!isLocationReady) {
            Log.d(TAG, "Location not ready, retrying in 1.5s...");
            new Handler(Looper.getMainLooper()).postDelayed(this::triggerEmergencyAction, 1500);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean autoCall = prefs.getBoolean(KEY_AUTO_CALL, true);
        boolean sendSms = prefs.getBoolean(KEY_SEND_SMS, true);
        boolean useSpeaker = prefs.getBoolean(KEY_USE_SPEAKER, false);

        String number1 = prefs.getString(KEY_NUMBER_1, "").trim();
        String number2 = prefs.getString(KEY_NUMBER_2, "").trim();
        String number3 = prefs.getString(KEY_NUMBER_3, "").trim();

        boolean number1Enabled = prefs.getBoolean(KEY_NUMBER_1_ENABLED, false);
        boolean number2Enabled = prefs.getBoolean(KEY_NUMBER_2_ENABLED, false);
        boolean number3Enabled = prefs.getBoolean(KEY_NUMBER_3_ENABLED, false);

        sendEmergencyActions(currentLocationUrl, autoCall, sendSms, useSpeaker,
                number1Enabled ? number1 : null,
                number2Enabled ? number2 : null,
                number3Enabled ? number3 : null);
    }

    private void sendEmergencyActions(String locationUrl, boolean autoCall, boolean sendSms, boolean useSpeaker,
                                      String num1, String num2, String num3) {

        Log.d(TAG, "Sending emergency actions... Location: " + locationUrl);
        String smsBody = "🚨 HELP! I need assistance.\nClick to navigate: " + locationUrl;

        // 📞 Auto-call
        if (autoCall && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
                String callNumber = null;
                if (num1 != null) callNumber = num1;
                else if (num2 != null) callNumber = num2;
                else if (num3 != null) callNumber = num3;

                if (callNumber != null && telecomManager != null) {
                    telecomManager.placeCall(Uri.fromParts("tel", callNumber, null), null);
                    Log.d(TAG, "Calling: " + callNumber);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error making call: " + e.getMessage());
            }
        }

        // 💬 SMS
        if (sendSms && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            try {
                ArrayList<String> parts = smsManager.divideMessage(smsBody);
                if (num1 != null) smsManager.sendMultipartTextMessage(num1, null, parts, null, null);
                if (num2 != null) smsManager.sendMultipartTextMessage(num2, null, parts, null, null);
                if (num3 != null) smsManager.sendMultipartTextMessage(num3, null, parts, null, null);
                Log.d(TAG, "SMS sent to emergency contacts");
            } catch (Exception e) {
                Log.e(TAG, "Error sending SMS: " + e.getMessage());
            }
        }
    }

    // ---------------- Notification ----------------

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafetyApp Active")
                .setContentText("Listening for 'help' keyword")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Trigger Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
