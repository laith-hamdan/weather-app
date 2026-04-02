package com.example.weatherapp.model;

import androidx.annotation.NonNull;

public final class WeatherSnapshot {
    public final String rawTimeIso;
    public final String shortTimeLabel;
    public final double temperatureC;
    public final int humidityPct;
    public final double precipitationMm;
    public final int cloudCoverPct;
    public final double windSpeedKmh;
    public final double visibilityMeters;
    public final int weatherCode;

    public WeatherSnapshot(
            @NonNull String rawTimeIso,
            @NonNull String shortTimeLabel,
            double temperatureC,
            int humidityPct,
            double precipitationMm,
            int cloudCoverPct,
            double windSpeedKmh,
            double visibilityMeters,
            int weatherCode) {
        this.rawTimeIso = rawTimeIso;
        this.shortTimeLabel = shortTimeLabel;
        this.temperatureC = temperatureC;
        this.humidityPct = humidityPct;
        this.precipitationMm = precipitationMm;
        this.cloudCoverPct = cloudCoverPct;
        this.windSpeedKmh = windSpeedKmh;
        this.visibilityMeters = visibilityMeters;
        this.weatherCode = weatherCode;
    }
}
