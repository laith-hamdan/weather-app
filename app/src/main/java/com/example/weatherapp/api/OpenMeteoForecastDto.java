package com.example.weatherapp.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenMeteoForecastDto {
    public double latitude;
    public double longitude;

    @SerializedName("hourly")
    public Hourly hourly;

    public static class Hourly {
        public List<String> time;
        public List<Double> temperature_2m;
        public List<Double> relative_humidity_2m;
        public List<Double> precipitation;
        public List<Double> cloudcover;
        public List<Double> wind_speed_10m;
        public List<Double> visibility;
        public List<Double> weathercode;
    }
}
