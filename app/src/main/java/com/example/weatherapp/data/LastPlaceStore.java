package com.example.weatherapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weatherapp.model.Place;

public final class LastPlaceStore {
    private static final String PREFS = "last_place";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_FROM_GPS = "from_gps";

    private final SharedPreferences prefs;

    public LastPlaceStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(@NonNull Place place) {
        prefs.edit()
                .putString(KEY_NAME, place.displayName)
                .putString(KEY_LAT, String.valueOf(place.latitude))
                .putString(KEY_LON, String.valueOf(place.longitude))
                .putBoolean(KEY_FROM_GPS, place.fromDeviceLocation)
                .apply();
    }

    @Nullable
    public Place load() {
        if (!prefs.contains(KEY_LAT)) {
            return null;
        }
        String name = prefs.getString(KEY_NAME, "");
        double lat = Double.parseDouble(prefs.getString(KEY_LAT, "0"));
        double lon = Double.parseDouble(prefs.getString(KEY_LON, "0"));
        boolean fromGps = prefs.getBoolean(KEY_FROM_GPS, false);
        return new Place(name != null ? name : "", lat, lon, fromGps);
    }
}
