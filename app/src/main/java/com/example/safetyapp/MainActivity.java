package com.example.safetyapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SafetyAppPrefs";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Switch switchAutoCall, switchSendSms, switchSpeaker;
    private Button startButton;

    private EditText inputNumber1, inputNumber2, inputNumber3;
    private CheckBox checkNumber1, checkNumber2, checkNumber3;

    private boolean isListening = false;
    private Intent voiceServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init views
        switchAutoCall = findViewById(R.id.switchAutoCall);
        switchSendSms = findViewById(R.id.switchSendSms);
        switchSpeaker = findViewById(R.id.switchSpeaker);
        startButton = findViewById(R.id.startButton);

        inputNumber1 = findViewById(R.id.inputNumber1);
        inputNumber2 = findViewById(R.id.inputNumber2);
        inputNumber3 = findViewById(R.id.inputNumber3);

        checkNumber1 = findViewById(R.id.checkNumber1);
        checkNumber2 = findViewById(R.id.checkNumber2);
        checkNumber3 = findViewById(R.id.checkNumber3);

        voiceServiceIntent = new Intent(this, VoiceTriggerService.class);

        // Load saved preferences
        loadPreferences();

        // Switch listeners
        switchAutoCall.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("auto_call", isChecked));
        switchSendSms.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("send_sms", isChecked));
        switchSpeaker.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("use_speaker", isChecked));

        // EditText watchers
        setupEditTextWatcher(inputNumber1, "number1");
        setupEditTextWatcher(inputNumber2, "number2");
        setupEditTextWatcher(inputNumber3, "number3");

        // CheckBox listeners
        checkNumber1.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("number1_enabled", isChecked));
        checkNumber2.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("number2_enabled", isChecked));
        checkNumber3.setOnCheckedChangeListener((buttonView, isChecked) -> savePreference("number3_enabled", isChecked));

        // Start/Stop Voice Detection
        startButton.setOnClickListener(v -> {
            if (!isListening) {
                if (checkAndRequestPermissions()) {
                    startVoiceService();
                }
            } else {
                stopService(voiceServiceIntent);
                Toast.makeText(MainActivity.this, "Voice Detection Stopped", Toast.LENGTH_SHORT).show();
                isListening = false;
                startButton.setText("Start Voice Detection");
            }
        });
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        switchAutoCall.setChecked(prefs.getBoolean("auto_call", true));
        switchSendSms.setChecked(prefs.getBoolean("send_sms", true));
        switchSpeaker.setChecked(prefs.getBoolean("use_speaker", false));

        inputNumber1.setText(prefs.getString("number1", ""));
        inputNumber2.setText(prefs.getString("number2", ""));
        inputNumber3.setText(prefs.getString("number3", ""));

        checkNumber1.setChecked(prefs.getBoolean("number1_enabled", false));
        checkNumber2.setChecked(prefs.getBoolean("number2_enabled", false));
        checkNumber3.setChecked(prefs.getBoolean("number3_enabled", false));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    private void savePreference(String key, String value) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    private void setupEditTextWatcher(EditText editText, String prefKey) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                savePreference(prefKey, s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
        };

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void startVoiceService() {
        voiceServiceIntent.putExtra("auto_call", switchAutoCall.isChecked());
        voiceServiceIntent.putExtra("send_sms", switchSendSms.isChecked());
        voiceServiceIntent.putExtra("use_speaker", switchSpeaker.isChecked());
        ContextCompat.startForegroundService(this, voiceServiceIntent);
        Toast.makeText(this, "Voice Detection Started", Toast.LENGTH_SHORT).show();
        isListening = true;
        startButton.setText("Stop Voice Detection");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean grantedAll = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    grantedAll = false;
                    break;
                }
            }
            if (grantedAll && !isListening) {
                startVoiceService();
            } else if (!grantedAll) {
                Toast.makeText(this, "Permissions denied. Voice detection won't start.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
