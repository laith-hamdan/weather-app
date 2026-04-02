package com.example.weatherapp.api;

import androidx.annotation.Nullable;

/**
 * Subset of Nominatim reverse JSON.
 */
class NominatimReverseDto {
    @Nullable String display_name;

    @Nullable AddressPart address;

    static class AddressPart {
        @Nullable String city;
        @Nullable String town;
        @Nullable String village;
        @Nullable String municipality;
        @Nullable String county;
        @Nullable String state;
        @Nullable String country;
    }
}
