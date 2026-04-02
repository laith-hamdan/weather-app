package com.example.weatherapp.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.weatherapp.R;

public final class WeatherCodeMapper {
    private WeatherCodeMapper() {}

    @NonNull
    public static String description(@NonNull Context context, int code) {
        int id = R.string.wmo_generic;
        if (code == 0) {
            id = R.string.wmo_clear;
        } else if (code == 1) {
            id = R.string.wmo_mainly_clear;
        } else if (code == 2) {
            id = R.string.wmo_partly_cloudy;
        } else if (code == 3) {
            id = R.string.wmo_overcast;
        } else if (code == 45 || code == 48) {
            id = R.string.wmo_fog;
        } else if (code >= 51 && code <= 57) {
            id = R.string.wmo_drizzle;
        } else if (code >= 61 && code <= 67) {
            id = R.string.wmo_rain;
        } else if (code >= 71 && code <= 77) {
            id = R.string.wmo_snow;
        } else if (code >= 80 && code <= 82) {
            id = R.string.wmo_rain_showers;
        } else if (code >= 85 && code <= 86) {
            id = R.string.wmo_snow_showers;
        } else if (code >= 95 && code <= 99) {
            id = R.string.wmo_thunderstorm;
        }
        return context.getString(id);
    }

    public static int iconRes(int code) {
        if (code == 0 || code == 1) return R.drawable.ic_weather_clear;
        if (code == 2) return R.drawable.ic_weather_partly_cloudy;
        if (code == 3) return R.drawable.ic_weather_cloudy;
        if (code == 45 || code == 48) return R.drawable.ic_weather_fog;
        if (code >= 51 && code <= 67) return R.drawable.ic_weather_rain;
        if (code >= 71 && code <= 77 || code >= 85 && code <= 86) return R.drawable.ic_weather_snow;
        if (code >= 80 && code <= 82) return R.drawable.ic_weather_rain;
        if (code >= 95 && code <= 99) return R.drawable.ic_weather_thunder;
        return R.drawable.ic_weather_cloudy;
    }
}
