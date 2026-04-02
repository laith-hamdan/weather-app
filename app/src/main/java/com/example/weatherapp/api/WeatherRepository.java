package com.example.weatherapp.api;

import androidx.annotation.NonNull;

import com.example.weatherapp.model.ParsedForecast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class WeatherRepository {
    private static final String FORECAST_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String GEO_BASE = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String NOMINATIM_SEARCH = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE = "https://nominatim.openstreetmap.org/reverse";

    /** Required by Nominatim usage policy (identify the application). */
    private static final String NOMINATIM_USER_AGENT = "WeatherApp/1.0 (com.example.weatherapp)";

    private final OkHttpClient client;
    private final Gson gson;
    private final Type nominatimListType = new TypeToken<List<NominatimPlaceDto>>() {}.getType();

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
        if (query == null) {
            return new ArrayList<>();
        }
        String q = query.trim();
        if (q.isEmpty() || LocaleScriptUtil.codePointCount(q) < 2) {
            return new ArrayList<>();
        }
        if (LocaleScriptUtil.containsArabicScript(q)) {
            return searchNominatim(q);
        }
        return searchOpenMeteo(q);
    }

    private List<GeocodingResponseDto.Result> searchOpenMeteo(String q) throws IOException {
        HttpUrl url =
                Objects.requireNonNull(HttpUrl.parse(GEO_BASE))
                        .newBuilder()
                        .addQueryParameter("name", q)
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

    private List<GeocodingResponseDto.Result> searchNominatim(String q) throws IOException {
        HttpUrl url =
                Objects.requireNonNull(HttpUrl.parse(NOMINATIM_SEARCH))
                        .newBuilder()
                        .addQueryParameter("q", q)
                        .addQueryParameter("format", "json")
                        .addQueryParameter("limit", "10")
                        .addQueryParameter("addressdetails", "0")
                        .build();
        Request request =
                new Request.Builder()
                        .url(url)
                        .header("User-Agent", NOMINATIM_USER_AGENT)
                        .get()
                        .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Nominatim HTTP " + response.code());
            }
            String body = response.body().string();
            List<NominatimPlaceDto> list = gson.fromJson(body, nominatimListType);
            if (list == null) {
                return new ArrayList<>();
            }
            List<GeocodingResponseDto.Result> out = new ArrayList<>();
            for (NominatimPlaceDto p : list) {
                GeocodingResponseDto.Result r = mapNominatimPlace(p);
                if (r != null) {
                    out.add(r);
                }
            }
            return out;
        }
    }

    private static GeocodingResponseDto.Result mapNominatimPlace(NominatimPlaceDto p) {
        if (p == null || p.lat == null || p.lon == null) {
            return null;
        }
        try {
            double lat = Double.parseDouble(p.lat);
            double lon = Double.parseDouble(p.lon);
            GeocodingResponseDto.Result r = new GeocodingResponseDto.Result();
            r.latitude = lat;
            r.longitude = lon;
            String display = p.displayName != null ? p.displayName.trim() : "";
            String[] parts = display.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            if (parts.length > 0 && !parts[0].isEmpty()) {
                r.name = p.name != null && !p.name.isEmpty() ? p.name : parts[0];
            } else {
                r.name = p.name != null ? p.name : "";
            }
            if (parts.length >= 2) {
                r.country = parts[parts.length - 1];
                r.admin1 = parts.length >= 3 ? parts[parts.length - 2] : "";
            } else {
                r.country = "";
                r.admin1 = "";
            }
            return r;
        } catch (NumberFormatException e) {
            return null;
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

    /**
     * Localized place line for coordinates using the given locale for {@code Accept-Language} /
     * {@code accept-language}.
     */
    public String reverseGeocodeLocalizedLabel(
            double latitude, double longitude, @NonNull Locale locale) throws IOException {
        String langTag = locale.toLanguageTag();
        HttpUrl url =
                Objects.requireNonNull(HttpUrl.parse(NOMINATIM_REVERSE))
                        .newBuilder()
                        .addQueryParameter("lat", String.valueOf(latitude))
                        .addQueryParameter("lon", String.valueOf(longitude))
                        .addQueryParameter("format", "json")
                        .addQueryParameter("addressdetails", "1")
                        .addQueryParameter("accept-language", langTag)
                        .build();
        Request request =
                new Request.Builder()
                        .url(url)
                        .header("User-Agent", NOMINATIM_USER_AGENT)
                        .header("Accept-Language", langTag)
                        .get()
                        .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Nominatim reverse HTTP " + response.code());
            }
            String body = response.body().string();
            NominatimReverseDto dto = gson.fromJson(body, NominatimReverseDto.class);
            return formatNominatimReverseLabel(dto);
        }
    }

    private static String formatNominatimReverseLabel(NominatimReverseDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto.address != null) {
            NominatimReverseDto.AddressPart a = dto.address;
            String locality =
                    firstNonEmpty(
                            a.city, a.town, a.village, a.municipality, a.county, a.state);
            if (locality != null && a.country != null && !a.country.isEmpty()) {
                return locality + ", " + a.country;
            }
            if (locality != null) {
                return locality;
            }
            if (a.state != null
                    && a.country != null
                    && !a.state.isEmpty()
                    && !a.country.isEmpty()) {
                return a.state + ", " + a.country;
            }
        }
        if (dto.display_name != null && !dto.display_name.trim().isEmpty()) {
            return dto.display_name.trim();
        }
        return null;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }
}
