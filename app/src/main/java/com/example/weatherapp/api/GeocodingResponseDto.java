package com.example.weatherapp.api;

import java.util.List;

public class GeocodingResponseDto {
    public List<Result> results;

    public static class Result {
        public String name;
        public String admin1;
        public String country;
        public double latitude;
        public double longitude;
    }
}
