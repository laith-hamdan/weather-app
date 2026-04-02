package com.example.weatherapp.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.databinding.ItemHourBinding;
import com.example.weatherapp.model.WeatherSnapshot;
import com.example.weatherapp.util.WeatherCodeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.Holder> {

    private final List<WeatherSnapshot> data = new ArrayList<>();

    public void submit(List<WeatherSnapshot> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHourBinding b =
                ItemHourBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemHourBinding binding;

        Holder(ItemHourBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WeatherSnapshot s) {
            binding.textTime.setText(s.shortTimeLabel);
            binding.textTemp.setText(
                    String.format(Locale.getDefault(), "%.0f°", s.temperatureC));
            binding.imageIcon.setImageResource(WeatherCodeMapper.iconRes(s.weatherCode));
            binding.imageIcon.setContentDescription(
                    WeatherCodeMapper.description(binding.getRoot().getContext(), s.weatherCode));
        }
    }
}
