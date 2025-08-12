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

    private Switch switchAutoCall, switchSendSms, switchSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchAutoCall = findViewById(R.id.switchAutoCall);
        switchSendSms = findViewById(R.id.switchSendSms);
        switchSpeaker = findViewById(R.id.switchSpeaker);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // ✅ Load saved settings
        switchAutoCall.setChecked(prefs.getBoolean(KEY_AUTO_CALL, true));
        switchSendSms.setChecked(prefs.getBoolean(KEY_SMS, true));
        switchSpeaker.setChecked(prefs.getBoolean(KEY_SPEAKER, false));

        // ✅ Save settings when toggled
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_AUTO_CALL, switchAutoCall.isChecked());
            editor.putBoolean(KEY_SMS, switchSendSms.isChecked());
            editor.putBoolean(KEY_SPEAKER, switchSpeaker.isChecked());
            editor.apply();
        };

        switchAutoCall.setOnCheckedChangeListener(listener);
        switchSendSms.setOnCheckedChangeListener(listener);
        switchSpeaker.setOnCheckedChangeListener(listener);
    }
}
