package com.example.weatherapp.model;

import androidx.annotation.NonNull;

import java.util.UUID;

public final class FavoritePlace {
    public String id;
    public String displayName;
    public double latitude;
    public double longitude;

    public FavoritePlace() {}

    public FavoritePlace(
            @NonNull String id,
            @NonNull String displayName,
            double latitude,
            double longitude) {
        this.id = id;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static FavoritePlace create(@NonNull String displayName, double latitude, double longitude) {
        return new FavoritePlace(UUID.randomUUID().toString(), displayName, latitude, longitude);
    }
}
