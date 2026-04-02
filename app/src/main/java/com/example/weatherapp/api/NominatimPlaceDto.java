package com.example.weatherapp.api;

import com.google.gson.annotations.SerializedName;

/**
 * Subset of Nominatim JSON (https://nominatim.org/release-docs/latest/api/Search/).
 */
class NominatimPlaceDto {
    String lat;
    String lon;

    @SerializedName("display_name")
    String displayName;

    String name;
}
