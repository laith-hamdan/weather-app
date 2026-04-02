package com.example.weatherapp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.R;
import com.example.weatherapp.databinding.DialogSearchBinding;
import com.example.weatherapp.databinding.FragmentWeatherBinding;
import com.example.weatherapp.model.FavoritePlace;
import com.example.weatherapp.model.ParsedForecast;
import com.example.weatherapp.model.Place;
import com.example.weatherapp.model.WeatherSnapshot;
import com.example.weatherapp.util.WeatherCodeMapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class WeatherFragment extends Fragment {

    private FragmentWeatherBinding binding;
    private WeatherViewModel viewModel;
    private HourlyAdapter hourlyAdapter;
    private FavoritesAdapter favoritesAdapter;
    private SearchResultsAdapter searchAdapter;
    private androidx.appcompat.app.AlertDialog searchDialog;

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 450L;

    private final ActivityResultLauncher<String[]> requestLocationPermissions =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted =
                                Boolean.TRUE.equals(
                                        result.getOrDefault(
                                                Manifest.permission.ACCESS_FINE_LOCATION, false))
                                        || Boolean.TRUE.equals(
                                                result.getOrDefault(
                                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                                        false));
                        if (granted) {
                            binding.cardLocationBanner.setVisibility(View.GONE);
                            viewModel.onLocationPermissionGranted();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel =
                new ViewModelProvider(
                                requireActivity(),
                                ViewModelProvider.AndroidViewModelFactory.getInstance(
                                        requireActivity().getApplication()))
                        .get(WeatherViewModel.class);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                binding.weatherRoot,
                (v, insets) -> {
                    androidx.core.graphics.Insets bars =
                            insets.getInsets(
                                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
                    binding.appBar.setPadding(0, bars.top, 0, 0);
                    return insets;
                });

        if (requireActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            ((androidx.appcompat.app.AppCompatActivity) requireActivity())
                    .setSupportActionBar(binding.toolbar);
        }

        hourlyAdapter = new HourlyAdapter();
        LinearLayoutManager hourlyLayout =
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
        binding.recyclerHourly.setLayoutManager(hourlyLayout);
        binding.recyclerHourly.setAdapter(hourlyAdapter);

        favoritesAdapter =
                new FavoritesAdapter(
                        new FavoritesAdapter.Listener() {
                            @Override
                            public void onOpen(@NonNull FavoritePlace place) {
                                viewModel.selectFavorite(place);
                            }

                            @Override
                            public void onRemove(@NonNull FavoritePlace place) {
                                viewModel.removeFavorite(place.id);
                            }
                        });
        LinearLayoutManager favLayout =
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
        binding.recyclerFavorites.setLayoutManager(favLayout);
        binding.recyclerFavorites.setAdapter(favoritesAdapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.fetchForecastForCurrentPlace());
        binding.buttonEnableLocation.setOnClickListener(v -> requestLocationIfNeeded());
        binding.fabFavorite.setOnClickListener(v -> viewModel.addCurrentPlaceToFavorites());

        requireActivity()
                .addMenuProvider(
                        new MenuProvider() {
                            @Override
                            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inf) {
                                inf.inflate(R.menu.weather_menu, menu);
                            }

                            @Override
                            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                                if (item.getItemId() == R.id.action_search) {
                                    showSearchDialog();
                                    return true;
                                }
                                if (item.getItemId() == R.id.action_language) {
                                    showLanguageDialog();
                                    return true;
                                }
                                return false;
                            }
                        },
                        getViewLifecycleOwner(),
                        Lifecycle.State.RESUMED);

        observeViewModel();
        viewModel.onUiReady();
    }

    private void showLanguageDialog() {
        String[] items =
                new String[] {
                    getString(R.string.language_english),
                    getString(R.string.language_arabic),
                    getString(R.string.language_follow_system),
                };
        int checked = currentLanguageDialogIndex();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(
                        items,
                        checked,
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags("en"));
                                    break;
                                case 1:
                                    AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags("ar"));
                                    break;
                                default:
                                    AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.getEmptyLocaleList());
                                    break;
                            }
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private int currentLanguageDialogIndex() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales == null || locales.isEmpty()) {
            return 2;
        }
        Locale loc = locales.get(0);
        if (loc == null) {
            return 2;
        }
        String lang = loc.getLanguage();
        if ("ar".equals(lang)) {
            return 1;
        }
        if ("en".equals(lang)) {
            return 0;
        }
        return 2;
    }

    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            binding.cardLocationBanner.setVisibility(View.GONE);
            viewModel.onLocationPermissionGranted();
            return;
        }
        requestLocationPermissions.launch(
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
    }

    private void observeViewModel() {
        viewModel
                .getLoading()
                .observe(
                        getViewLifecycleOwner(),
                        loading -> {
                            if (Boolean.TRUE.equals(loading)) {
                                if (!binding.swipeRefresh.isRefreshing()) {
                                    binding.progress.setVisibility(View.VISIBLE);
                                }
                            } else {
                                binding.progress.setVisibility(View.GONE);
                                binding.swipeRefresh.setRefreshing(false);
                            }
                        });

        viewModel
                .getError()
                .observe(
                        getViewLifecycleOwner(),
                        msg -> {
                            if (msg != null && !msg.isEmpty()) {
                                binding.textError.setText(msg);
                                binding.textError.setVisibility(View.VISIBLE);
                            } else {
                                binding.textError.setVisibility(View.GONE);
                            }
                        });

        viewModel
                .getForecast()
                .observe(
                        getViewLifecycleOwner(),
                        f -> {
                            if (f != null) {
                                bindForecast(f);
                                binding.textError.setVisibility(View.GONE);
                            }
                        });

        viewModel
                .getPlace()
                .observe(
                        getViewLifecycleOwner(),
                        p -> {
                            if (p != null) {
                                binding.textPlaceName.setText(p.displayName);
                            }
                        });

        viewModel
                .getFavorites()
                .observe(
                        getViewLifecycleOwner(),
                        list -> favoritesAdapter.submit(list));

        viewModel
                .getLocationBanner()
                .observe(
                        getViewLifecycleOwner(),
                        show ->
                                binding.cardLocationBanner.setVisibility(
                                        Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));

        viewModel
                .getSearchResults()
                .observe(
                        getViewLifecycleOwner(),
                        results -> {
                            if (searchAdapter != null) {
                                searchAdapter.submit(results);
                            }
                        });
    }

    private void bindForecast(@NonNull ParsedForecast f) {
        WeatherSnapshot c = f.currentHour;
        binding.textTemperature.setText(String.format(Locale.getDefault(), "%.0f°", c.temperatureC));
        binding.textCondition.setText(WeatherCodeMapper.description(requireContext(), c.weatherCode));
        binding.imageHeroWeather.setImageResource(WeatherCodeMapper.iconRes(c.weatherCode));
        binding.imageHeroWeather.setContentDescription(
                WeatherCodeMapper.description(requireContext(), c.weatherCode));

        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        binding.textUpdated.setText(
                getString(R.string.updated_at, tf.format(new Date(f.fetchedAtEpochMs))));

        binding.textTodaySummary.setText(
                getString(
                        R.string.today_summary,
                        String.format(Locale.getDefault(), "%.0f°", f.todayMinTemp),
                        String.format(Locale.getDefault(), "%.0f°", f.todayMaxTemp),
                        String.format(Locale.getDefault(), "%.1f", f.todayPrecipitationSumMm),
                        String.format(Locale.getDefault(), "%.0f", f.todayMaxWindKmh)));

        binding.metricHumidity.setText(getString(R.string.metric_humidity, c.humidityPct));
        binding.metricWind.setText(getString(R.string.metric_wind, c.windSpeedKmh));
        binding.metricPrecip.setText(getString(R.string.metric_precip, c.precipitationMm));
        binding.metricCloud.setText(getString(R.string.metric_cloud, c.cloudCoverPct));
        double visKm = c.visibilityMeters > 0 ? c.visibilityMeters / 1000.0 : 0;
        binding.metricVisibility.setText(getString(R.string.metric_visibility, visKm));

        hourlyAdapter.submit(f.todayHourly);
    }

    private void showSearchDialog() {
        if (searchDialog != null && searchDialog.isShowing()) {
            return;
        }
        DialogSearchBinding d = DialogSearchBinding.inflate(getLayoutInflater());
        searchAdapter =
                new SearchResultsAdapter(
                        result -> {
                            viewModel.selectGeocodingResult(result);
                            if (searchDialog != null) {
                                searchDialog.dismiss();
                            }
                        });
        d.recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        d.recyclerResults.setAdapter(searchAdapter);

        searchDialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.search_title)
                        .setView(d.getRoot())
                        .setNegativeButton(android.R.string.cancel, (dialog, w) -> dialog.dismiss())
                        .create();

        d.inputSearch.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        runSearch(d);
                        return true;
                    }
                    return false;
                });

        d.inputSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (pendingSearchRunnable != null) {
                            searchDebounceHandler.removeCallbacks(pendingSearchRunnable);
                        }
                        final String text = s != null ? s.toString() : "";
                        pendingSearchRunnable =
                                () -> {
                                    if (meetsMinSearchLength(text)) {
                                        viewModel.searchPlaces(text);
                                    }
                                };
                        searchDebounceHandler.postDelayed(
                                pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
                    }
                });

        searchDialog.setOnDismissListener(
                dialog -> {
                    if (pendingSearchRunnable != null) {
                        searchDebounceHandler.removeCallbacks(pendingSearchRunnable);
                    }
                    pendingSearchRunnable = null;
                    searchAdapter = null;
                    searchDialog = null;
                });

        searchDialog.show();
    }

    private void runSearch(DialogSearchBinding d) {
        if (pendingSearchRunnable != null) {
            searchDebounceHandler.removeCallbacks(pendingSearchRunnable);
        }
        CharSequence q = d.inputSearch.getText();
        if (meetsMinSearchLength(q)) {
            viewModel.searchPlaces(q.toString());
        }
    }

    private static boolean meetsMinSearchLength(CharSequence s) {
        if (s == null) {
            return false;
        }
        String t = s.toString().trim();
        return t.codePointCount(0, t.length()) >= 2;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
