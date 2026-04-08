# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented (Espresso) tests
./gradlew lint                   # Run Android Lint
./gradlew clean                  # Clean build artifacts
```

Run a single test class:
```bash
./gradlew test --tests "com.example.weatherapp.ExampleUnitTest"
```

## Architecture

**MVVM** with a single `:app` module. All source is Java (not Kotlin) under `app/src/main/java/com/example/weatherapp/`.

### Layer overview

| Package | Role |
|---------|------|
| `ui/` | `WeatherFragment` (main screen) + `WeatherViewModel` + three `RecyclerView` adapters |
| `api/` | `WeatherRepository` (OkHttp calls) + DTOs + `ForecastParser` |
| `model/` | Plain data objects: `Place`, `FavoritePlace`, `ParsedForecast`, `WeatherSnapshot` |
| `data/` | SharedPreferences wrappers: `LastPlaceStore`, `FavoritesStore` |
| `location/` | `LocationHelper` (FusedLocationProvider), `PlaceNameLocalizer`, `PlaceLabelFormatter` |
| `util/` | `WeatherCodeMapper` (WMO codes → icon/text), `AppLocale` |

### Data flow

1. **Location bootstrap**: GPS → last saved place → first favorite → hardcoded default (Aalborg, Denmark)
2. **Search**: user query → `LocaleScriptUtil` detects Arabic script → routes to Nominatim (Arabic) or Open-Meteo Geocoding (everything else) → `searchResults` LiveData
3. **Forecast**: selected `Place` → Open-Meteo 10-day hourly API → `ForecastParser` → `forecast` LiveData

### Threading

All network/IO runs on a `newSingleThreadExecutor()`; results are posted back to the main thread via `Handler(Looper.getMainLooper())`. No coroutines or RxJava.

### External APIs (no API keys required)

- **Open-Meteo Weather** – 10-day hourly forecasts (WMO weather codes, UTC)
- **Open-Meteo Geocoding** – default place search
- **Nominatim (OSM)** – fallback for Arabic-script queries and reverse geocoding
- **Google Play Services FusedLocationProvider** – device GPS

### Key build facts

- `compileSdk`/`targetSdk` 36, `minSdk` 30
- ViewBinding enabled; no DataBinding
- Java 11 source compatibility
- Release builds have `isMinifyEnabled = false`
