package com.example.safetyapp;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        // ✅ Load user preference for speaker
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        boolean useSpeaker = prefs.getBoolean(SettingsActivity.KEY_SPEAKER, false);

        Log.d(TAG, "🔊 Speaker preference: " + useSpeaker);

        if (useSpeaker) {
            enableSpeakerphone(true);
        } else {
            enableSpeakerphone(false);
        }
    }

    private void enableSpeakerphone(boolean enable) {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(enable);
                Log.d(TAG, enable ? "🔊 Speakerphone ENABLED" : "🔇 Speakerphone DISABLED");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error toggling speaker: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Reset speaker to normal when leaving call
        enableSpeakerphone(false);
    }
}
