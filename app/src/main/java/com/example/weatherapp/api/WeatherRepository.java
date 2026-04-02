package com.example.weatherapp.api;

import com.example.weatherapp.model.ParsedForecast;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class WeatherRepository {
    private static final String FORECAST_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String GEO_BASE = "https://geocoding-api.open-meteo.com/v1/search";

    private final OkHttpClient client;
    private final Gson gson;

    public WeatherRepository() {
        this.client =
                new OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build();
        this.gson = new Gson();
    }

    public ParsedForecast loadForecast(double latitude, double longitude) throws IOException {
        HttpUrl url =
                Objects.requireNonNull(HttpUrl.parse(FORECAST_BASE))
                        .newBuilder()
                        .addQueryParameter("latitude", String.valueOf(latitude))
                        .addQueryParameter("longitude", String.valueOf(longitude))
                        .addQueryParameter(
                                "hourly",
                                "temperature_2m,relative_humidity_2m,precipitation,cloudcover,"
                                        + "wind_speed_10m,visibility,weathercode")
                        .addQueryParameter("forecast_days", "10")
                        .addQueryParameter("timezone", "UTC")
                        .addQueryParameter("models", "gfs_global")
                        .build();

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Forecast HTTP " + response.code());
            }
            String body = response.body().string();
            OpenMeteoForecastDto dto = gson.fromJson(body, OpenMeteoForecastDto.class);
            return ForecastParser.parse(dto);
        }
    }

    public List<GeocodingResponseDto.Result> searchPlaces(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        HttpUrl url =
                Objects.requireNonNull(HttpUrl.parse(GEO_BASE))
                        .newBuilder()
                        .addQueryParameter("name", query.trim())
                        .addQueryParameter("count", "10")
                        .build();
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Geocoding HTTP " + response.code());
            }
            String body = response.body().string();
            GeocodingResponseDto dto = gson.fromJson(body, GeocodingResponseDto.class);
            if (dto == null || dto.results == null) {
                return new ArrayList<>();
            }
            return dto.results;
        }
    }

    public static String formatGeocodingLabel(GeocodingResponseDto.Result r) {
        StringBuilder sb = new StringBuilder(r.name);
        if (r.admin1 != null && !r.admin1.isEmpty()) {
            sb.append(", ").append(r.admin1);
        }
        if (r.country != null && !r.country.isEmpty()) {
            sb.append(", ").append(r.country);
        }
        return sb.toString();
    }
}
