package com.example.weatherapp.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.api.GeocodingResponseDto;
import com.example.weatherapp.databinding.ItemSearchResultBinding;

import java.util.ArrayList;
import java.util.List;

public final class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.Holder> {

    public interface Listener {
        void onPick(@NonNull GeocodingResponseDto.Result result);
    }

    private final Listener listener;
    private final List<GeocodingResponseDto.Result> data = new ArrayList<>();

    public SearchResultsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(List<GeocodingResponseDto.Result> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchResultBinding b =
                ItemSearchResultBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemSearchResultBinding binding;

        Holder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GeocodingResponseDto.Result r) {
            binding.textPrimary.setText(r.name);
            StringBuilder sub = new StringBuilder();
            if (r.admin1 != null && !r.admin1.isEmpty()) {
                sub.append(r.admin1);
            }
            if (r.country != null && !r.country.isEmpty()) {
                if (sub.length() > 0) {
                    sub.append(" · ");
                }
                sub.append(r.country);
            }
            binding.textSecondary.setText(sub.length() > 0 ? sub.toString() : " ");
            binding.getRoot()
                    .setOnClickListener(v -> listener.onPick(r));
        }
    }
}
