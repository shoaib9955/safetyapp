package com.example.safetyapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class EmergencyUIActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("🚨 Calling & Sending Emergency SMS...");
        tv.setTextSize(24);
        tv.setPadding(50, 200, 50, 200);
        setContentView(tv);
    }
}
