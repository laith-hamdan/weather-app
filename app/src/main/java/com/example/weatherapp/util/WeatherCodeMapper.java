package com.example.weatherapp.util;

import com.example.weatherapp.R;

public final class WeatherCodeMapper {
    private WeatherCodeMapper() {}

    public static String description(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Weather";
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
