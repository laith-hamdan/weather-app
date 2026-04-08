package com.example.weatherapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.R;
import com.example.weatherapp.databinding.ItemDayBinding;
import com.example.weatherapp.model.DailyForecast;
import com.example.weatherapp.util.AppLocale;
import com.example.weatherapp.util.WeatherCodeMapper;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DailyAdapter extends RecyclerView.Adapter<DailyAdapter.Holder> {

    private final List<DailyForecast> data = new ArrayList<>();

    public void submit(List<DailyForecast> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDayBinding b =
                ItemDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemDayBinding binding;

        Holder(ItemDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DailyForecast day) {
            // Day label: "Today" for index 0, otherwise short weekday name in current locale
            if (day.isToday) {
                binding.textDayLabel.setText(R.string.label_today);
            } else {
                Locale locale = AppLocale.currentLocale();
                String[] shortNames = new DateFormatSymbols(locale).getShortWeekdays();
                // shortWeekdays[0] is empty; indices 1–7 map to Sun–Sat (Calendar constants)
                binding.textDayLabel.setText(shortNames[day.calendarDayOfWeek]);
            }

            binding.imageWeatherIcon.setImageResource(
                    WeatherCodeMapper.iconRes(day.dominantWeatherCode));
            binding.imageWeatherIcon.setContentDescription(
                    WeatherCodeMapper.description(binding.getRoot().getContext(),
                            day.dominantWeatherCode));

            binding.textTempRange.setText(
                    String.format(Locale.getDefault(), "%.0f° / %.0f°",
                            day.maxTempC, day.minTempC));

            if (day.precipSumMm > 0.1) {
                binding.textPrecipBadge.setVisibility(View.VISIBLE);
                binding.textPrecipBadge.setText(
                        String.format(Locale.getDefault(), "%.1f mm", day.precipSumMm));
            } else {
                binding.textPrecipBadge.setVisibility(View.GONE);
            }
        }
    }
}
