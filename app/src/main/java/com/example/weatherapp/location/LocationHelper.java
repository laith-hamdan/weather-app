package com.example.weatherapp.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.weatherapp.model.Place;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocationHelper {
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocationHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                                appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public interface Callback {
        void onPlace(@NonNull Place place);

        void onFailure();
    }

    public void fetchCurrentPlace(@NonNull Callback callback) {
        if (!hasLocationPermission()) {
            callback.onFailure();
            return;
        }
        FusedLocationProviderClient fused =
                LocationServices.getFusedLocationProviderClient(appContext);
        CancellationTokenSource cts = new CancellationTokenSource();
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(
                        location -> {
                            if (location == null) {
                                fused.getLastLocation()
                                        .addOnSuccessListener(
                                                loc2 -> {
                                                    if (loc2 == null) {
                                                        callback.onFailure();
                                                    } else {
                                                        resolveLabel(
                                                                loc2.getLatitude(),
                                                                loc2.getLongitude(),
                                                                callback);
                                                    }
                                                })
                                        .addOnFailureListener(e -> callback.onFailure());
                                return;
                            }
                            resolveLabel(
                                    location.getLatitude(), location.getLongitude(), callback);
                        })
                .addOnFailureListener(e -> callback.onFailure());
    }

    private void resolveLabel(double lat, double lon, @NonNull Callback callback) {
        executor.execute(
                () -> {
                    String label = formatCoordinates(lat, lon);
                    if (Geocoder.isPresent()) {
                        try {
                            Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String formatted =
                                        PlaceLabelFormatter.fromAddress(addresses.get(0));
                                if (formatted != null && !formatted.isEmpty()) {
                                    label = formatted;
                                }
                            }
                        } catch (IOException ignored) {
                        }
                    }
                    String finalLabel = label;
                    new android.os.Handler(Looper.getMainLooper())
                            .post(
                                    () ->
                                            callback.onPlace(
                                                    new Place(
                                                            finalLabel,
                                                            lat,
                                                            lon,
                                                            true)));
                });
    }

    @NonNull
    private static String formatCoordinates(double lat, double lon) {
        return String.format(Locale.US, "%.2f°, %.2f°", lat, lon);
    }
}
