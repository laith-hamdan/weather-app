package com.example.weatherapp.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherapp.api.GeocodingResponseDto;
import com.example.weatherapp.api.WeatherRepository;
import com.example.weatherapp.data.FavoritesStore;
import com.example.weatherapp.data.LastPlaceStore;
import com.example.weatherapp.location.LocationHelper;
import com.example.weatherapp.model.FavoritePlace;
import com.example.weatherapp.model.ParsedForecast;
import com.example.weatherapp.model.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WeatherViewModel extends AndroidViewModel {

    public static final double DEFAULT_LAT = 55.991;
    public static final double DEFAULT_LON = 8.354;

    private final WeatherRepository repository = new WeatherRepository();
    private final LastPlaceStore lastPlaceStore;
    private final FavoritesStore favoritesStore;
    private final LocationHelper locationHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<ParsedForecast> forecast = new MutableLiveData<>(null);
    private final MutableLiveData<Place> place = new MutableLiveData<>(null);
    private final MutableLiveData<List<FavoritePlace>> favorites = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> locationBanner = new MutableLiveData<>(false);
    private final MutableLiveData<List<GeocodingResponseDto.Result>> searchResults =
            new MutableLiveData<>(new ArrayList<>());

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        lastPlaceStore = new LastPlaceStore(application);
        favoritesStore = new FavoritesStore(application);
        locationHelper = new LocationHelper(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
        locationHelper.shutdown();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<ParsedForecast> getForecast() {
        return forecast;
    }

    public LiveData<Place> getPlace() {
        return place;
    }

    public LiveData<List<FavoritePlace>> getFavorites() {
        return favorites;
    }

    public LiveData<Boolean> getLocationBanner() {
        return locationBanner;
    }

    public LiveData<List<GeocodingResponseDto.Result>> getSearchResults() {
        return searchResults;
    }

    public void refreshFavoritesList() {
        favorites.setValue(favoritesStore.loadAll());
    }

    public void bootstrapFromLocationOrFallback() {
        refreshFavoritesList();
        if (locationHelper.hasLocationPermission()) {
            locationBanner.setValue(false);
            loading.setValue(true);
            error.setValue(null);
            locationHelper.fetchCurrentPlace(
                    new LocationHelper.Callback() {
                        @Override
                        public void onPlace(@NonNull Place p) {
                            applyPlaceAndFetch(p);
                        }

                        @Override
                        public void onFailure() {
                            loadFallbackPlace();
                        }
                    });
        } else {
            locationBanner.setValue(true);
            loading.setValue(true);
            error.setValue(null);
            loadFallbackPlace();
        }
    }

    private void loadFallbackPlace() {
        Place last = lastPlaceStore.load();
        if (last != null) {
            applyPlaceAndFetch(last);
            return;
        }
        List<FavoritePlace> fav = favoritesStore.loadAll();
        if (!fav.isEmpty()) {
            FavoritePlace f = fav.get(0);
            applyPlaceAndFetch(
                    new Place(f.displayName, f.latitude, f.longitude, false));
            return;
        }
        applyPlaceAndFetch(
                new Place(
                        getApplication().getString(com.example.weatherapp.R.string.default_area_name),
                        DEFAULT_LAT,
                        DEFAULT_LON,
                        false));
    }

    public void onLocationPermissionGranted() {
        locationBanner.setValue(false);
        loading.setValue(true);
        error.setValue(null);
        locationHelper.fetchCurrentPlace(
                new LocationHelper.Callback() {
                    @Override
                    public void onPlace(@NonNull Place p) {
                        applyPlaceAndFetch(p);
                    }

                    @Override
                    public void onFailure() {
                        mainHandler.post(() -> loadFallbackPlace());
                    }
                });
    }

    private void applyPlaceAndFetch(@NonNull Place p) {
        lastPlaceStore.save(p);
        place.setValue(p);
        fetchForecastForCurrentPlace();
    }

    public void fetchForecastForCurrentPlace() {
        Place p = place.getValue();
        if (p == null) {
            return;
        }
        loading.setValue(true);
        error.setValue(null);
        executor.execute(
                () -> {
                    try {
                        ParsedForecast f = repository.loadForecast(p.latitude, p.longitude);
                        mainHandler.post(
                                () -> {
                                    forecast.setValue(f);
                                    loading.setValue(false);
                                    error.setValue(null);
                                });
                    } catch (Exception e) {
                        mainHandler.post(
                                () -> {
                                    loading.setValue(false);
                                    error.setValue(
                                            e.getMessage() != null
                                                    ? e.getMessage()
                                                    : getApplication()
                                                            .getString(
                                                                    com.example.weatherapp
                                                                            .R
                                                                            .string
                                                                            .error_generic));
                                });
                    }
                });
    }

    public void selectPlace(@NonNull Place p) {
        lastPlaceStore.save(p);
        place.setValue(p);
        fetchForecastForCurrentPlace();
    }

    public void selectGeocodingResult(@NonNull GeocodingResponseDto.Result r) {
        String label = WeatherRepository.formatGeocodingLabel(r);
        selectPlace(new Place(label, r.latitude, r.longitude, false));
    }

    public void selectFavorite(@NonNull FavoritePlace f) {
        selectPlace(new Place(f.displayName, f.latitude, f.longitude, false));
    }

    public void addCurrentPlaceToFavorites() {
        Place p = place.getValue();
        if (p == null) {
            return;
        }
        favoritesStore.add(FavoritePlace.create(p.displayName, p.latitude, p.longitude));
        refreshFavoritesList();
    }

    public void removeFavorite(@NonNull String id) {
        favoritesStore.removeById(id);
        refreshFavoritesList();
    }

    public void searchPlaces(@NonNull String query) {
        executor.execute(
                () -> {
                    try {
                        List<GeocodingResponseDto.Result> r = repository.searchPlaces(query);
                        mainHandler.post(() -> searchResults.setValue(r));
                    } catch (Exception e) {
                        mainHandler.post(() -> searchResults.setValue(new ArrayList<>()));
                    }
                });
    }
}
