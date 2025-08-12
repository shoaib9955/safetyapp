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

public class VoiceTriggerService extends Service {

    private static final String TAG = "VoiceTrigger";
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_trigger_channel";

    private final String emergencyNumber = "1234567890"; // ✅ Replace with actual number
    private final String smsNumber = "1234567890";       // ✅ Replace with actual number

    private PowerManager.WakeLock wakeLock;
    private int helpCount = 0;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ✅ Keep CPU awake
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafetyApp::VoiceWakeLock");
        wakeLock.acquire();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { Log.d(TAG, "Ready for speech"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Log.e(TAG, "Speech error: " + error);
                new Handler(Looper.getMainLooper()).postDelayed(() -> restartListening(), 500);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String result : matches) {
                        Log.d(TAG, "Recognized: " + result);
                        if (result.toLowerCase().contains("help")) {
                            helpCount++;
                            if (helpCount >= 1) {
                                triggerEmergencyAction();
                                helpCount = 0;
                                break;
                            }
                        }
                    }
                }
                restartListening();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

        startListening();
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
        Log.d(TAG, "🚨 HELP DETECTED! Checking user settings...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendEmergencyActions("Location not available");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String locMsg = (location != null)
                    ? "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude()
                    : "Location not available";
            sendEmergencyActions(locMsg);
        });
    }

    private void sendEmergencyActions(String locationMsg) {
        // ✅ Load user preferences
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        boolean autoCall = prefs.getBoolean(SettingsActivity.KEY_AUTO_CALL, true);
        boolean useSpeaker = prefs.getBoolean(SettingsActivity.KEY_SPEAKER, false);
        boolean sendSms = prefs.getBoolean(SettingsActivity.KEY_SMS, true);

        Log.d(TAG, "Settings -> AutoCall: " + autoCall + ", Speaker: " + useSpeaker + ", SMS: " + sendSms);

        // ✅ 1️⃣ Make call if enabled
        if (autoCall && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
                if (telecomManager != null) {
                    Log.d(TAG, "📞 Placing emergency call to " + emergencyNumber);
                    telecomManager.placeCall(Uri.fromParts("tel", emergencyNumber, null), null);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error making call: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "📞 AutoCall disabled or permission missing");
        }

        // ✅ 2️⃣ Send SMS if enabled
        if (sendSms && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            String smsBody = "🚨 HELP! I need assistance. My location: " + locationMsg;
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(smsNumber, null, smsBody, null, null);
            Log.d(TAG, "📩 SMS sent with location");
        } else {
            Log.d(TAG, "📩 SMS disabled or permission missing");
        }

        // ✅ 3️⃣ Handle speaker mode
        if (useSpeaker) {
            Log.d(TAG, "🔊 Speaker mode ON (you can integrate with CallActivity if needed)");
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafetyApp Active")
                .setContentText("Listening for 'HELP' even when locked")
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
