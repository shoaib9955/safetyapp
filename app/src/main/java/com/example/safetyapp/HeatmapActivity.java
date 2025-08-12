package com.example.safetyapp;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;

public class HeatmapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng cityCenter = new LatLng(37.7749, -122.4194); // Example: San Francisco
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityCenter, 12));

        loadCrimeData();
    }

    private void loadCrimeData() {
        List<LatLng> locations = new ArrayList<>();
        locations.add(new LatLng(37.7749, -122.4194));
        locations.add(new LatLng(37.7849, -122.4094));
        locations.add(new LatLng(37.7649, -122.4294));
        locations.add(new LatLng(37.7549, -122.4194));

        if (locations.isEmpty()) {
            Toast.makeText(this, "No crime data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 Custom gradient (green -> yellow -> red)
        int[] colors = {
                0xFF00FF00, // green
                0xFFFFFF00, // yellow
                0xFFFF0000  // red
        };
        float[] startPoints = {0.2f, 0.5f, 1f};
        Gradient gradient = new Gradient(colors, startPoints);

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(locations)
                .gradient(gradient)
                .build();

        heatmapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
    }

    // ✅ Call this to update heatmap dynamically with new data
    private void updateHeatmap(List<LatLng> newLocations) {
        if (heatmapProvider != null) {
            heatmapProvider.setData(newLocations);
            heatmapOverlay.clearTileCache();
        }
    }
}
