package com.example.safetyapp.utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakeCrimeDatabase {

    public static class CrimePoint {
        public LatLng location;
        public double intensity; // 0.2=low,0.5=medium,1.0=high

        public CrimePoint(LatLng location, double intensity) {
            this.location = location;
            this.intensity = intensity;
        }
    }

    /**
     * Generate clustered crime points for distinct zones
     * @param center Center of the map
     * @param radiusMeters Maximum distance from center
     */
    public static List<CrimePoint> generateCrimePoints(LatLng center, double radiusMeters) {
        List<CrimePoint> points = new ArrayList<>();
        Random random = new Random();

        // Define clusters: low, medium, high
        double[][] clusterOffsets = {
                {0.0, 0.0},      // low cluster near center
                {0.15, 0.15},    // medium cluster offset NE
                {-0.15, -0.15}   // high cluster offset SW
        };

        double[] intensities = {0.2, 0.5, 1.0};
        int pointsPerZone = 70; // ~210 points total

        for (int zone = 0; zone < 3; zone++) {
            for (int i = 0; i < pointsPerZone; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radiusMeters / 3; // each cluster smaller radius
                double dx = distance * Math.cos(angle) / 111000f;
                double dy = distance * Math.sin(angle) / (111000f * Math.cos(Math.toRadians(center.latitude)));

                double newLat = center.latitude + dy + clusterOffsets[zone][0];
                double newLng = center.longitude + dx + clusterOffsets[zone][1];

                points.add(new CrimePoint(new LatLng(newLat, newLng), intensities[zone]));
            }
        }

        return points;
    }
}
