package com.example.safetyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "✅ Device rebooted - starting services");

            // Start VoiceTriggerService
            Intent voiceIntent = new Intent(context, VoiceTriggerService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(voiceIntent);
            } else {
                context.startService(voiceIntent);
            }

            // Start LocationService
            Intent locationIntent = new Intent(context, LocationService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(locationIntent);
            } else {
                context.startService(locationIntent);
            }
        }
    }
}
