package com.example.weatherapp.model;

import androidx.annotation.NonNull;

public final class Place {
    public final String displayName;
    public final double latitude;
    public final double longitude;
    public final boolean fromDeviceLocation;

    public Place(
            @NonNull String displayName,
            double latitude,
            double longitude,
            boolean fromDeviceLocation) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fromDeviceLocation = fromDeviceLocation;
    }
}
