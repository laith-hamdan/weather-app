package com.example.weatherapp.api;

import com.example.weatherapp.model.DailyForecast;
import com.example.weatherapp.model.ParsedForecast;
import com.example.weatherapp.model.WeatherSnapshot;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        // Group hourly indices by calendar date (first 10 chars of ISO string)
        LinkedHashMap<String, List<Integer>> byDay = groupIndicesByDay(times);

        LocalDate todayUtc = ZonedDateTime.now(UTC).toLocalDate();
        List<DailyForecast> tenDayForecast = new ArrayList<>();
        List<WeatherSnapshot> todayHourly = new ArrayList<>();
        double todayMinT = Double.POSITIVE_INFINITY;
        double todayMaxT = Double.NEGATIVE_INFINITY;
        double todayPrecipSum = 0;
        double todayMaxWind = 0;

        int dayIndex = 0;
        for (Map.Entry<String, List<Integer>> entry : byDay.entrySet()) {
            if (dayIndex >= 10) break;
            String dateStr = entry.getKey();
            List<Integer> indices = entry.getValue();

            double minT = Double.POSITIVE_INFINITY;
            double maxT = Double.NEGATIVE_INFINITY;
            double precipSum = 0;
            double maxWind = 0;
            List<WeatherSnapshot> daySnaps = new ArrayList<>();

            for (int idx : indices) {
                WeatherSnapshot snap = buildSnapshot(dto, idx);
                daySnaps.add(snap);
                minT = Math.min(minT, snap.temperatureC);
                maxT = Math.max(maxT, snap.temperatureC);
                precipSum += snap.precipitationMm;
                maxWind = Math.max(maxWind, snap.windSpeedKmh);
            }

            if (daySnaps.isEmpty()) {
                dayIndex++;
                continue;
            }

            int dominantCode = dominantWeatherCode(dto.hourly.weathercode, indices);
            LocalDate date = LocalDate.parse(dateStr);
            int calDow = toCalendarDayOfWeek(date.getDayOfWeek());
            boolean isToday = date.equals(todayUtc);

            tenDayForecast.add(new DailyForecast(
                    dateStr, calDow, minT, maxT, dominantCode, precipSum, maxWind, isToday));

            if (isToday) {
                todayHourly = daySnaps;
                todayMinT = minT;
                todayMaxT = maxT;
                todayPrecipSum = precipSum;
                todayMaxWind = maxWind;
            }
            dayIndex++;
        }

        // Fallback if today wasn't found in grouped data
        if (todayHourly.isEmpty()) {
            WeatherSnapshot only = buildSnapshot(dto, currentIdx);
            todayHourly.add(only);
            todayMinT = todayMaxT = only.temperatureC;
            todayPrecipSum = only.precipitationMm;
            todayMaxWind = only.windSpeedKmh;
        }

        WeatherSnapshot current = buildSnapshot(dto, currentIdx);
        return new ParsedForecast(
                current,
                todayHourly,
                todayMinT,
                todayMaxT,
                todayPrecipSum,
                todayMaxWind,
                current.weatherCode,
                System.currentTimeMillis(),
                tenDayForecast);
    }

    /** Groups hourly time-string indices into day buckets, preserving chronological order. */
    private static LinkedHashMap<String, List<Integer>> groupIndicesByDay(List<String> times) {
        LinkedHashMap<String, List<Integer>> map = new LinkedHashMap<>();
        for (int i = 0; i < times.size(); i++) {
            String dateKey = times.get(i).substring(0, 10); // "2026-04-08"
            if (!map.containsKey(dateKey)) {
                map.put(dateKey, new ArrayList<>());
            }
            map.get(dateKey).add(i);
        }
        return map;
    }

    /**
     * Returns the most frequent weather code in the given index set.
     * Ties are broken by choosing the highest (most severe) code.
     */
    private static int dominantWeatherCode(List<Double> weathercodes, List<Integer> indices) {
        if (weathercodes == null || weathercodes.isEmpty()) return 0;
        HashMap<Integer, Integer> freq = new HashMap<>();
        for (int idx : indices) {
            if (idx < weathercodes.size()) {
                Double v = weathercodes.get(idx);
                int code = v == null ? 0 : (int) Math.round(v);
                freq.put(code, freq.getOrDefault(code, 0) + 1);
            }
        }
        int best = 0;
        int bestFreq = -1;
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            if (e.getValue() > bestFreq || (e.getValue() == bestFreq && e.getKey() > best)) {
                best = e.getKey();
                bestFreq = e.getValue();
            }
        }
        return best;
    }

    /**
     * Converts java.time DayOfWeek (ISO: Mon=1 … Sun=7) to
     * java.util.Calendar day-of-week (Sun=1 … Sat=7) used by DateFormatSymbols.
     */
    private static int toCalendarDayOfWeek(DayOfWeek dow) {
        return (dow.getValue() % 7) + 1;
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
