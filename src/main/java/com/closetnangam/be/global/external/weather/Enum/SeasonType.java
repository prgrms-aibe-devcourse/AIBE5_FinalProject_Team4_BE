package com.closetnangam.be.global.external.weather.Enum;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public enum SeasonType {

    COLD("COLD", "WINTER", "ALL", "ALL_SEASON"),
    MILD("MILD", "SPRING", "AUTUMN", "FALL", "TRANSITION", "ALL", "ALL_SEASON"),
    HOT("HOT", "SUMMER", "ALL", "ALL_SEASON");

    private final List<String> searchLabels;

    SeasonType(String... aliases) {
        this.searchLabels = Arrays.stream(aliases)
                .map(alias -> alias.toUpperCase(Locale.ROOT))
                .toList();
    }

    public Collection<String> getSearchLabels() {
        return searchLabels;
    }

    public boolean matches(String season) {
        if (season == null) return false;
        return searchLabels.contains(season.trim().toUpperCase(Locale.ROOT));
    }
}