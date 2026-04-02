package com.example.weatherapp.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.databinding.ItemFavoriteBinding;
import com.example.weatherapp.model.FavoritePlace;

import java.util.ArrayList;
import java.util.List;

public final class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull FavoritePlace place);

        void onRemove(@NonNull FavoritePlace place);
    }

    private final Listener listener;
    private final List<FavoritePlace> data = new ArrayList<>();

    public FavoritesAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(List<FavoritePlace> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFavoriteBinding b =
                ItemFavoriteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemFavoriteBinding binding;

        Holder(ItemFavoriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FavoritePlace f) {
            binding.textName.setText(f.displayName);
            binding.getRoot()
                    .setOnClickListener(v -> listener.onOpen(f));
            binding.buttonRemove.setOnClickListener(v -> listener.onRemove(f));
        }
    }
}
