package com.example.safetyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "SafetyAppPrefs";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";

    private Switch switchVoiceTrigger;
    private Button btnSettings;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        switchVoiceTrigger = findViewById(R.id.switchVoiceTrigger);
        btnSettings = findViewById(R.id.btnSettings);

        // ✅ Restore last state
        boolean isVoiceEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, false);
        switchVoiceTrigger.setChecked(isVoiceEnabled);
        if (isVoiceEnabled) {
            startService(new Intent(this, VoiceTriggerService.class));
        }

        // ✅ Toggle voice service on/off
        switchVoiceTrigger.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_VOICE_ENABLED, isChecked).apply();
            if (isChecked) {
                startService(new Intent(this, VoiceTriggerService.class));
            } else {
                stopService(new Intent(this, VoiceTriggerService.class));
            }
        });

        // ✅ Open settings
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
    }
}
