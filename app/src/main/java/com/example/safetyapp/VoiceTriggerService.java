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
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceTriggerService extends Service {

    private static final String TAG = "VoiceTriggerService";
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_trigger_channel";

    private final String emergencyNumber = "1234567890"; // replace with actual emergency number

    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient fusedLocationClient;

    // Preference keys (same keys as toggles in MainActivity)
    private static final String PREFS_NAME = "SafetyAppPrefs";
    private static final String KEY_AUTO_CALL = "auto_call";
    private static final String KEY_SEND_SMS = "send_sms";
    private static final String KEY_USE_SPEAKER = "use_speaker";

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
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override
            public void onError(int error) {
                Log.e(TAG, "SpeechRecognizer error: " + error);
                new Handler(Looper.getMainLooper()).postDelayed(VoiceTriggerService.this::restartListening, 500);
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String result : matches) {
                        Log.d(TAG, "Recognized phrase: " + result);
                        if (result.toLowerCase(Locale.ROOT).contains("help")) {
                            Log.d(TAG, "Help detected!");
                            triggerEmergencyAction();
                            break;
                        }
                    }
                }
                restartListening();
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
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

    private void triggerEmergencyAction() {
        Log.d(TAG, "Triggering emergency actions...");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean autoCall = prefs.getBoolean(KEY_AUTO_CALL, true);
        boolean sendSms = prefs.getBoolean(KEY_SEND_SMS, true);
        boolean useSpeaker = prefs.getBoolean(KEY_USE_SPEAKER, false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // No location permission, send actions without location
            sendEmergencyActions("Location not available", autoCall, sendSms, useSpeaker);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String locMsg = (location != null)
                    ? "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude()
                    : "Location not available";
            sendEmergencyActions(locMsg, autoCall, sendSms, useSpeaker);
        });
    }

    private void sendEmergencyActions(String locationMsg, boolean autoCall, boolean sendSms, boolean useSpeaker) {
        Log.d(TAG, "Settings -> autoCall: " + autoCall + ", sendSms: " + sendSms + ", useSpeaker: " + useSpeaker);

        // 1️⃣ Make call if enabled and permission granted
        if (autoCall && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
                if (telecomManager != null) {
                    Log.d(TAG, "📞 Calling emergency number: " + emergencyNumber);
                    telecomManager.placeCall(Uri.fromParts("tel", emergencyNumber, null), null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error making emergency call: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Auto-call disabled or CALL_PHONE permission missing");
        }

        // 2️⃣ Send SMS if enabled and permission granted
        if (sendSms && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            String smsBody = "🚨 HELP! I need assistance. My location: " + locationMsg;
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyNumber, null, smsBody, null, null);
            Log.d(TAG, "SMS sent to emergency contact");
        } else {
            Log.d(TAG, "Send SMS disabled or SEND_SMS permission missing");
        }

        // 3️⃣ Handle speaker mode if enabled
        if (useSpeaker) {
            Log.d(TAG, "Speaker mode enabled (implement as needed)");
            // Add speaker mode logic if required (e.g., enable speakerphone during call)
        }
    }

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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
