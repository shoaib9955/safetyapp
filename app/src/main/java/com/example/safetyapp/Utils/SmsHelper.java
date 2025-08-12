package com.example.safetyapp;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsHelper {

    public static void sendSMS(Context context, String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(context, "SMS Sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
