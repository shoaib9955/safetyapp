package com.example.safetyapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

public class CallHelper {

    public static void makeEmergencyCall(Context context, String phoneNumber) {
        try {
            // ✅ Check CALL_PHONE permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Call permission not granted!", Toast.LENGTH_LONG).show();
                return;
            }

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to make call: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
