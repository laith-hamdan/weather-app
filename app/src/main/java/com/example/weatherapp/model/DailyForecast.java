package com.example.weatherapp.model;

public final class DailyForecast {
    public final String dateIso;         // "2026-04-08"
    public final int calendarDayOfWeek;  // Calendar.SUNDAY … Calendar.SATURDAY (1–7)
    public final double minTempC;
    public final double maxTempC;
    public final int dominantWeatherCode;
    public final double precipSumMm;
    public final double maxWindKmh;
    public final boolean isToday;

    public DailyForecast(
            String dateIso,
            int calendarDayOfWeek,
            double minTempC,
            double maxTempC,
            int dominantWeatherCode,
            double precipSumMm,
            double maxWindKmh,
            boolean isToday) {
        this.dateIso = dateIso;
        this.calendarDayOfWeek = calendarDayOfWeek;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
        this.dominantWeatherCode = dominantWeatherCode;
        this.precipSumMm = precipSumMm;
        this.maxWindKmh = maxWindKmh;
        this.isToday = isToday;
    }
}
