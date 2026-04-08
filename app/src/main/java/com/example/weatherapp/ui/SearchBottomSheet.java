package com.example.weatherapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.api.GeocodingResponseDto;
import com.example.weatherapp.databinding.FragmentSearchBottomSheetBinding;
import com.example.weatherapp.model.FavoritePlace;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public final class SearchBottomSheet extends BottomSheetDialogFragment {

    public interface OnPlaceSelectedListener {
        void onPlaceSelected(@NonNull GeocodingResponseDto.Result result);
    }

    private FragmentSearchBottomSheetBinding binding;
    private WeatherViewModel viewModel;
    private SearchResultsAdapter searchAdapter;
    private FavoritesAdapter favoritesAdapter;
    private OnPlaceSelectedListener listener;

    public void setOnPlaceSelectedListener(@NonNull OnPlaceSelectedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBottomSheetBinding.inflate(inflater, container, false);
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

        searchAdapter =
                new SearchResultsAdapter(
                        result -> {
                            if (listener != null) {
                                listener.onPlaceSelected(result);
                            }
                            dismiss();
                        });
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSearchResults.setAdapter(searchAdapter);

        favoritesAdapter =
                new FavoritesAdapter(
                        new FavoritesAdapter.Listener() {
                            @Override
                            public void onOpen(@NonNull FavoritePlace place) {
                                viewModel.selectFavorite(place);
                                dismiss();
                            }

                            @Override
                            public void onRemove(@NonNull FavoritePlace place) {
                                viewModel.removeFavorite(place.id);
                            }
                        });
        binding.recyclerFavoritesSheet.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.recyclerFavoritesSheet.setAdapter(favoritesAdapter);

        // Observe favorites and search results from shared ViewModel
        viewModel
                .getFavorites()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            favoritesAdapter.submit(list);
                            boolean hasFavorites = list != null && !list.isEmpty();
                            boolean isQueryEmpty = currentQuery().isEmpty();
                            if (isQueryEmpty) {
                                setFavoritesVisible(hasFavorites);
                            }
                        });

        viewModel
                .getSearchResults()
                .observe(
                        getViewLifecycleOwner(),
                        results -> {
                            if (!currentQuery().isEmpty()) {
                                searchAdapter.submit(results);
                                boolean hasResults = results != null && !results.isEmpty();
                                binding.recyclerSearchResults.setVisibility(
                                        hasResults ? View.VISIBLE : View.GONE);
                                binding.textEmptyState.setVisibility(
                                        hasResults ? View.GONE : View.VISIBLE);
                            }
                        });

        binding.inputSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String query = s != null ? s.toString() : "";
                        onQueryChanged(query);
                    }
                });

        binding.inputSearch.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String q = currentQuery();
                        if (!q.isEmpty()) {
                            viewModel.submitSearchQuery(q);
                        }
                        return true;
                    }
                    return false;
                });

        // Show favorites panel initially if there are any
        showEmptyQueryState();

        // Show keyboard
        binding.inputSearch.requestFocus();
        binding.inputSearch.postDelayed(this::showKeyboard, 100);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand to full height immediately
        View bottomSheet = getDialog().findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.clearSearchResults();
        binding = null;
    }

    private void onQueryChanged(String query) {
        if (query.isEmpty()) {
            showEmptyQueryState();
        } else {
            showSearchingState();
            viewModel.submitSearchQuery(query);
        }
    }

    private void showEmptyQueryState() {
        List<FavoritePlace> favs = viewModel.getFavorites().getValue();
        boolean hasFavs = favs != null && !favs.isEmpty();
        setFavoritesVisible(hasFavs);
        binding.recyclerSearchResults.setVisibility(View.GONE);
        binding.textEmptyState.setVisibility(View.GONE);
    }

    private void showSearchingState() {
        setFavoritesVisible(false);
        binding.recyclerSearchResults.setVisibility(View.GONE);
        binding.textEmptyState.setVisibility(View.GONE);
    }

    private void setFavoritesVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        binding.textSavedLabel.setVisibility(vis);
        binding.recyclerFavoritesSheet.setVisibility(vis);
        binding.dividerSheet.setVisibility(vis);
    }

    private String currentQuery() {
        Editable e = binding.inputSearch.getText();
        return e != null ? e.toString() : "";
    }

    private void showKeyboard() {
        if (binding == null) return;
        InputMethodManager imm =
                (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.inputSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
