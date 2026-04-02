package com.example.weatherapp.api;

import com.example.weatherapp.model.ParsedForecast;
import com.example.weatherapp.model.WeatherSnapshot;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ForecastParser {
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private ForecastParser() {}

    public static ParsedForecast parse(OpenMeteoForecastDto dto) throws IOException {
        if (dto == null || dto.hourly == null || dto.hourly.time == null || dto.hourly.time.isEmpty()) {
            throw new IOException("Invalid forecast payload");
        }
        List<String> times = dto.hourly.time;
        int n = times.size();

        ZonedDateTime nowUtcHour =
                ZonedDateTime.now(UTC).withMinute(0).withSecond(0).withNano(0);
        int currentIdx = findCurrentHourIndex(times, nowUtcHour);
        if (currentIdx < 0) {
            currentIdx = n - 1;
        }

        LocalDate todayUtc = ZonedDateTime.now(UTC).toLocalDate();
        List<WeatherSnapshot> today = new ArrayList<>();
        double minT = Double.POSITIVE_INFINITY;
        double maxT = Double.NEGATIVE_INFINITY;
        double precipSum = 0;
        double maxWind = 0;

        for (int i = 0; i < n; i++) {
            ZonedDateTime z = parseUtc(times.get(i));
            if (!z.toLocalDate().equals(todayUtc)) {
                continue;
            }
            WeatherSnapshot snap = buildSnapshot(dto, i);
            today.add(snap);
            minT = Math.min(minT, snap.temperatureC);
            maxT = Math.max(maxT, snap.temperatureC);
            precipSum += snap.precipitationMm;
            maxWind = Math.max(maxWind, snap.windSpeedKmh);
        }

        if (today.isEmpty()) {
            WeatherSnapshot only = buildSnapshot(dto, currentIdx);
            today.add(only);
            minT = maxT = only.temperatureC;
            precipSum = only.precipitationMm;
            maxWind = only.windSpeedKmh;
        }

        WeatherSnapshot current = buildSnapshot(dto, currentIdx);
        return new ParsedForecast(
                current,
                today,
                minT,
                maxT,
                precipSum,
                maxWind,
                current.weatherCode,
                System.currentTimeMillis());
    }

    private static int findCurrentHourIndex(List<String> times, ZonedDateTime nowUtcHour) {
        for (int i = 0; i < times.size(); i++) {
            ZonedDateTime t = parseUtc(times.get(i));
            if (!t.isBefore(nowUtcHour)) {
                return i;
            }
        }
        return -1;
    }

    private static ZonedDateTime parseUtc(String iso) {
        return java.time.LocalDateTime.parse(iso, ISO_LOCAL).atZone(UTC);
    }

    private static WeatherSnapshot buildSnapshot(OpenMeteoForecastDto dto, int i) {
        OpenMeteoForecastDto.Hourly h = dto.hourly;
        String raw = h.time.get(i);
        ZonedDateTime utc = parseUtc(raw);
        String label =
                utc.withZoneSameInstant(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));

        double temp = d(h.temperature_2m, i);
        int hum = (int) Math.round(d(h.relative_humidity_2m, i));
        double precip = d(h.precipitation, i);
        int cloud = (int) Math.round(d(h.cloudcover, i));
        double wind = d(h.wind_speed_10m, i);
        double vis = d(h.visibility, i);
        int code = (int) Math.round(d(h.weathercode, i));

        return new WeatherSnapshot(raw, label, temp, hum, precip, cloud, wind, vis, code);
    }

    private static double d(List<Double> list, int i) {
        if (list == null || i >= list.size() || i < 0) {
            return 0;
        }
        Double v = list.get(i);
        return v == null ? 0 : v;
    }
}
