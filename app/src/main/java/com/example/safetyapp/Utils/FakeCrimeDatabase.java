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
     * Generate mixed crime zones near the user
     * @param center Center location
     * @param radiusMeters Max radius
     * @param totalZones Number of zones
     */
    public static List<CrimePoint> generateCrimePoints(LatLng center, double radiusMeters, int totalZones) {
        List<CrimePoint> points = new ArrayList<>();
        Random random = new Random();

        double[] intensities = {0.2, 0.5, 1.0}; // green, yellow, red

        for (int i = 0; i < totalZones; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radiusMeters;
            double dx = distance * Math.cos(angle) / 111000f;
            double dy = distance * Math.sin(angle) / (111000f * Math.cos(Math.toRadians(center.latitude)));

            double newLat = center.latitude + dy;
            double newLng = center.longitude + dx;

            double intensity = intensities[random.nextInt(intensities.length)];
            points.add(new CrimePoint(new LatLng(newLat, newLng), intensity));
        }

        return points;
    }
}
