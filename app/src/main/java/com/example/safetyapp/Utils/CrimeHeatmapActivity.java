package com.example.safetyapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrimeHeatmapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<CrimePoint> fakePoints = new ArrayList<>();
    private TileOverlay heatmapOverlay;
    private final List<Circle> zoneCircles = new ArrayList<>();

    private Button btnToggleMap, btnToggleSatellite, btnSetNumber, btnSmsToggle, btnVoiceToggle;
    private TextView tvEmergencyNumber;
    private boolean showingHeatmap = false;
    private boolean satelliteMode = false;

    private Marker userMarker;
    private List<LatLng> userTrail = new ArrayList<>();
    private Polyline userPolyline;

    private ZoneNotifier zoneNotifier;

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int REQUEST_SMS_PERMISSION = 101;

    private Button btnToggleDropdown;
    private LinearLayout dropdownContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_heatmap);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Controls
        btnToggleMap = findViewById(R.id.btnToggleMap);
        btnToggleSatellite = findViewById(R.id.btnToggleSatellite);
        btnSetNumber = findViewById(R.id.btnSetNumber);
        btnSmsToggle = findViewById(R.id.btnToggleSMS);
        btnVoiceToggle = findViewById(R.id.btnToggleVoice);

        tvEmergencyNumber = findViewById(R.id.tvEmergencyNumber);
        btnToggleDropdown = findViewById(R.id.btnToggleDropdown);
        dropdownContainer = findViewById(R.id.dropdownContainer);

        updateEmergencyNumberUI();
        updateToggleButtonUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        btnToggleDropdown.setOnClickListener(v -> {
            if (dropdownContainer.getVisibility() == LinearLayout.GONE) {
                dropdownContainer.setVisibility(LinearLayout.VISIBLE);
                btnToggleDropdown.setText("Hide Controls ▲");
            } else {
                dropdownContainer.setVisibility(LinearLayout.GONE);
                btnToggleDropdown.setText("Show Controls ▼");
            }
        });

        btnToggleMap.setOnClickListener(v -> {
            showingHeatmap = !showingHeatmap;
            if (showingHeatmap) showHeatmap();
            else showZonesFast();
        });

        btnToggleSatellite.setOnClickListener(v -> {
            satelliteMode = !satelliteMode;
            if (mMap != null)
                mMap.setMapType(satelliteMode ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
        });

        btnSetNumber.setOnClickListener(v -> promptEmergencyNumber());

        btnSmsToggle.setOnClickListener(v -> toggleButtonPreference("smsEnabled", btnSmsToggle, "SMS"));
        btnVoiceToggle.setOnClickListener(v -> toggleButtonPreference("voiceEnabled", btnVoiceToggle, "Voice"));

        zoneNotifier = new ZoneNotifier(this);

        setupLocationUpdates();
        requestSmsPermission();

        Intent serviceIntent = new Intent(this, LocationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateToggleButtonUI() {
        boolean smsEnabled = getPref("smsEnabled", true);
        boolean voiceEnabled = getPref("voiceEnabled", true);

        btnSmsToggle.setText("SMS: " + (smsEnabled ? "ON" : "OFF"));
        btnVoiceToggle.setText("Voice: " + (voiceEnabled ? "ON" : "OFF"));
    }

    private void toggleButtonPreference(String key, Button button, String name) {
        boolean enabled = getPref(key, true);
        enabled = !enabled;
        setPref(key, enabled);
        button.setText(name + ": " + (enabled ? "ON" : "OFF"));
        Toast.makeText(this, name + " " + (enabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    private boolean getPref(String key, boolean defaultVal) {
        return getSharedPreferences("safetyAppPrefs", MODE_PRIVATE)
                .getBoolean(key, defaultVal);
    }

    private void setPref(String key, boolean value) {
        getSharedPreferences("safetyAppPrefs", MODE_PRIVATE)
                .edit().putBoolean(key, value).apply();
    }

    private void updateEmergencyNumberUI() {
        String number = getSharedPreferences("safetyAppPrefs", MODE_PRIVATE)
                .getString("emergencyNumber", "No number saved");
        tvEmergencyNumber.setText("SMS will be sent to: " + number);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        fetchUserLocation();
    }

    private void setupLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && mMap != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (userMarker == null) {
                        userMarker = mMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("You are here"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f));
                    } else {
                        userMarker.setPosition(currentLatLng);
                    }

                    userTrail.add(currentLatLng);
                    if (userPolyline != null) userPolyline.remove();
                    userPolyline = mMap.addPolyline(new PolylineOptions()
                            .addAll(userTrail)
                            .width(5)
                            .color(0xFF0000FF));

                    zoneNotifier.checkZoneEntry(currentLatLng, fakePoints);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }
        fusedLocationClient.requestLocationUpdates(
                LocationRequest.create()
                        .setInterval(5000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                locationCallback,
                null
        );
    }

    private void fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, this::onLocationReady);
    }

    private void onLocationReady(Location location) {
        if (location == null) return;

        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        fakePoints.clear();
        double radiusMeters = 10000;
        int totalPoints = 50;

        fakePoints.addAll(generateFakePoints(userLatLng, radiusMeters, totalPoints));

        getSharedPreferences("safetyAppPrefs", MODE_PRIVATE).edit()
                .putString("fixedZones", CrimePoint.toJsonArray(fakePoints))
                .apply();

        showZonesFast();
        startLocationUpdates();
    }

    private List<CrimePoint> generateFakePoints(LatLng center, double radiusM, int totalPoints) {
        List<CrimePoint> points = new ArrayList<>();
        Random random = new Random();
        double[] intensities = {0.2, 0.5, 1.0};
        double minGap = 300;

        for (int i = 0; i < totalPoints; i++) {
            boolean validPoint = false;
            LatLng newPoint = null;

            while (!validPoint) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radiusM;
                double dx = distance * Math.cos(angle) / 111000f;
                double dy = distance * Math.sin(angle) / (111000f * Math.cos(Math.toRadians(center.latitude)));

                double lat = center.latitude + dy;
                double lng = center.longitude + dx;
                newPoint = new LatLng(lat, lng);

                validPoint = true;
                for (CrimePoint p : points) {
                    float[] result = new float[1];
                    android.location.Location.distanceBetween(p.location.latitude, p.location.longitude,
                            lat, lng, result);
                    if (result[0] < minGap) {
                        validPoint = false;
                        break;
                    }
                }
            }

            double intensity = intensities[i % 3];
            points.add(new CrimePoint(newPoint, intensity));
        }
        return points;
    }

    private void showZonesFast() {
        if (heatmapOverlay != null) { heatmapOverlay.remove(); heatmapOverlay = null; }
        for (Circle c : zoneCircles) c.remove();
        zoneCircles.clear();

        new Thread(() -> {
            for (CrimePoint p : fakePoints) {
                runOnUiThread(() -> drawCircle(p.location, p.intensity));
                try { Thread.sleep(5); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void drawCircle(LatLng location, double intensity) {
        int fill, stroke;
        float radius = 150f;
        float strokeWidth = 2f;

        if (intensity < 0.3) { fill = 0x5500FF00; stroke = 0xFF00FF00; }
        else if (intensity < 0.8) { fill = 0x55FFFF00; stroke = 0xFFFFFF00; }
        else { fill = 0x55FF0000; stroke = 0xFFFF0000; }

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(location)
                .radius(radius)
                .strokeWidth(strokeWidth)
                .strokeColor(stroke)
                .fillColor(fill));
        zoneCircles.add(circle);
    }

    private void showHeatmap() {
        for (Circle c : zoneCircles) c.remove();
        zoneCircles.clear();

        List<LatLng> points = new ArrayList<>();
        for (CrimePoint p : fakePoints) points.add(p.location);

        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .data(points)
                .radius(50)
                .build();
        heatmapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }

    private void promptEmergencyNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Emergency Number");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String number = input.getText().toString().trim();
            if (!number.isEmpty()) {
                getSharedPreferences("safetyAppPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("emergencyNumber", number)
                        .apply();
                Toast.makeText(this, "Number saved!", Toast.LENGTH_SHORT).show();
                updateEmergencyNumberUI();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void requestSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_SMS_PERMISSION);
        }
    }

    static class CrimePoint {
        LatLng location;
        double intensity;

        CrimePoint(LatLng location, double intensity) {
            this.location = location;
            this.intensity = intensity;
        }

        static String toJsonArray(List<CrimePoint> points) {
            JSONArray array = new JSONArray();
            try {
                for (CrimePoint p : points) {
                    JSONObject obj = new JSONObject();
                    obj.put("lat", p.location.latitude);
                    obj.put("lng", p.location.longitude);
                    obj.put("intensity", p.intensity);
                    array.put(obj);
                }
            } catch (Exception e) { e.printStackTrace(); }
            return array.toString();
        }

        static List<CrimePoint> fromJsonArray(String json) {
            List<CrimePoint> points = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    LatLng loc = new LatLng(obj.getDouble("lat"), obj.getDouble("lng"));
                    double intensity = obj.getDouble("intensity");
                    points.add(new CrimePoint(loc, intensity));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return points;
        }
    }
}
