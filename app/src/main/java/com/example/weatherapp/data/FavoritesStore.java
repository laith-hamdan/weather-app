package com.example.weatherapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.weatherapp.model.FavoritePlace;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FavoritesStore {
    private static final String PREFS = "favorites_store";
    private static final String KEY_JSON = "json";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<ArrayList<FavoritePlace>>() {}.getType();

    public FavoritesStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public List<FavoritePlace> loadAll() {
        String json = prefs.getString(KEY_JSON, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        List<FavoritePlace> list = gson.fromJson(json, listType);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void saveAll(@NonNull List<FavoritePlace> places) {
        prefs.edit().putString(KEY_JSON, gson.toJson(places)).apply();
    }

    public void add(@NonNull FavoritePlace place) {
        List<FavoritePlace> list = loadAll();
        for (Iterator<FavoritePlace> it = list.iterator(); it.hasNext(); ) {
            FavoritePlace f = it.next();
            if (Math.abs(f.latitude - place.latitude) < 1e-5
                    && Math.abs(f.longitude - place.longitude) < 1e-5) {
                it.remove();
            }
        }
        list.add(0, place);
        saveAll(list);
    }

    public void removeById(@NonNull String id) {
        List<FavoritePlace> list = loadAll();
        list.removeIf(f -> id.equals(f.id));
        saveAll(list);
    }
}
