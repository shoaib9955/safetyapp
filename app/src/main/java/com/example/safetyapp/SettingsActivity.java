package com.example.safetyapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "SafetyAppPrefs";
    public static final String KEY_AUTO_CALL = "auto_call";
    public static final String KEY_SMS = "send_sms";
    public static final String KEY_SPEAKER = "use_speaker";
    public static final String KEY_PRIORITY_CONTACT = "priority_contact";


    private Switch switchAutoCall, switchSendSms, switchSpeaker;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences and Editor once
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        editor = prefs.edit();

        switchAutoCall = findViewById(R.id.switchAutoCall);
        switchSendSms = findViewById(R.id.switchSendSms);
        switchSpeaker = findViewById(R.id.switchSpeaker);

        // Load saved preferences and set switches accordingly
        switchAutoCall.setChecked(prefs.getBoolean(KEY_AUTO_CALL, true));
        switchSendSms.setChecked(prefs.getBoolean(KEY_SMS, true));
        switchSpeaker.setChecked(prefs.getBoolean(KEY_SPEAKER, false));

        // Set individual listeners to save preferences immediately on change
        switchAutoCall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_AUTO_CALL, isChecked);
            editor.apply();
        });

        switchSendSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_SMS, isChecked);
            editor.apply();
        });

        switchSpeaker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_SPEAKER, isChecked);
            editor.apply();
        });
    }
}
