package com.example.weatherapp.location;

import android.location.Address;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PlaceLabelFormatter {
    private PlaceLabelFormatter() {}

    @Nullable
    public static String fromAddress(@NonNull Address a) {
        StringBuilder sb = new StringBuilder();
        if (a.getLocality() != null) {
            sb.append(a.getLocality());
        } else if (a.getSubAdminArea() != null) {
            sb.append(a.getSubAdminArea());
        } else if (a.getFeatureName() != null) {
            sb.append(a.getFeatureName());
        }
        if (a.getCountryName() != null && sb.length() > 0) {
            sb.append(", ").append(a.getCountryName());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
