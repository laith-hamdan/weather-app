package com.example.weatherapp.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weatherapp.api.WeatherRepository;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Resolves a short place label for coordinates in the given {@link Locale} (e.g. app UI language).
 * Prefers Nominatim reverse, then Android {@link Geocoder}.
 */
public final class PlaceNameLocalizer {
    private PlaceNameLocalizer() {}

    @Nullable
    public static String resolve(
            @NonNull Context appContext,
            @NonNull WeatherRepository repository,
            double latitude,
            double longitude,
            @NonNull Locale locale) {
        try {
            String fromNom =
                    repository.reverseGeocodeLocalizedLabel(latitude, longitude, locale);
            if (fromNom != null && !fromNom.isEmpty()) {
                return fromNom.trim();
            }
        } catch (IOException ignored) {
        }
        if (Geocoder.isPresent()) {
            try {
                Geocoder geocoder = new Geocoder(appContext, locale);
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String s = PlaceLabelFormatter.fromAddress(addresses.get(0));
                    if (s != null && !s.isEmpty()) {
                        return s;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
