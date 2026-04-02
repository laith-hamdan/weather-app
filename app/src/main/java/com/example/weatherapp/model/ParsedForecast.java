package com.example.weatherapp.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public final class ParsedForecast {
    public final WeatherSnapshot currentHour;
    @NonNull
    public final List<WeatherSnapshot> todayHourly;
    public final double todayMinTemp;
    public final double todayMaxTemp;
    public final double todayPrecipitationSumMm;
    public final double todayMaxWindKmh;
    public final int headlineWeatherCode;
    public final long fetchedAtEpochMs;

    public ParsedForecast(
            @NonNull WeatherSnapshot currentHour,
            @NonNull List<WeatherSnapshot> todayHourly,
            double todayMinTemp,
            double todayMaxTemp,
            double todayPrecipitationSumMm,
            double todayMaxWindKmh,
            int headlineWeatherCode,
            long fetchedAtEpochMs) {
        this.currentHour = currentHour;
        this.todayHourly = Collections.unmodifiableList(todayHourly);
        this.todayMinTemp = todayMinTemp;
        this.todayMaxTemp = todayMaxTemp;
        this.todayPrecipitationSumMm = todayPrecipitationSumMm;
        this.todayMaxWindKmh = todayMaxWindKmh;
        this.headlineWeatherCode = headlineWeatherCode;
        this.fetchedAtEpochMs = fetchedAtEpochMs;
    }
}
