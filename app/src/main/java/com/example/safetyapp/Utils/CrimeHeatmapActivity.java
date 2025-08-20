package com.example.safetyapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrimeHeatmapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userMarker;
    private LocationCallback locationCallback;

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_heatmap);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Toggle button for map type
        Button toggleMapButton = findViewById(R.id.btnToggleMap);
        toggleMapButton.setOnClickListener(v -> {
            if (mMap != null) {
                if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // 5 sec
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUserLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void updateUserLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Add or move marker
        if (userMarker == null) {
            BitmapDescriptor personIcon = BitmapDescriptorFactory.fromBitmap(getBitmapFromVector(R.drawable.ic_person));
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("You are here")
                    .icon(personIcon));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13));
        } else {
            userMarker.setPosition(userLatLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
        }

        // Update heatmap
        List<LatLng> crimePoints = generateCrimePointsAround(location, 100, 30000);
        addHeatmap(crimePoints);
        checkIfInCrimeZone(location, crimePoints);
    }

    private Bitmap getBitmapFromVector(int vectorResId) {
        Drawable vectorDrawable = getResources().getDrawable(vectorResId, null);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private List<LatLng> generateCrimePointsAround(Location center, int count, double radiusMeters) {
        List<LatLng> points = new ArrayList<>();
        Random random = new Random();

        double latCenter = center.getLatitude();
        double lngCenter = center.getLongitude();

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radiusMeters;
            double dx = distance * Math.cos(angle) / 111000f;
            double dy = distance * Math.sin(angle) / (111000f * Math.cos(Math.toRadians(latCenter)));
            points.add(new LatLng(latCenter + dx, lngCenter + dy));
        }

        return points;
    }

    private void addHeatmap(List<LatLng> locations) {
        int[] colors = {0xFF33CC33, 0xFFFFCC00, 0xFFFF3300};
        float[] startPoints = {0.2f, 0.5f, 1f};
        Gradient gradient = new Gradient(colors, startPoints);

        if (heatmapOverlay != null) heatmapOverlay.remove();

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(locations)
                .gradient(gradient)
                .build();

        heatmapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
    }

    private void checkIfInCrimeZone(Location userLocation, List<LatLng> crimePoints) {
        for (LatLng point : crimePoints) {
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    point.latitude, point.longitude, distance
            );

            if (distance[0] <= 50) {
                Toast.makeText(this, "⚠️ You are entering a high-crime area!", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
