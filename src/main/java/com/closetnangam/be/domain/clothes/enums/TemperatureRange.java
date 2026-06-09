package com.closetnangam.be.domain.clothes.enums;

public enum TemperatureRange {

    COLD,   // 겨울  (~4°C)
    CHILLY, // 늦가을/초겨울 (5~11°C)
    MILD,   // 봄/가을  (12~19°C)
    WARM,   // 초여름/초가을  (20~27°C)
    HOT;    // 여름  (28°C~)

    public static TemperatureRange from(double currentTemp) {
        if (currentTemp < 5.0d)  return COLD;
        if (currentTemp < 12.0d) return CHILLY;
        if (currentTemp < 20.0d) return MILD;
        if (currentTemp < 28.0d) return WARM;
        return HOT;
    }

    public boolean requiresOuter() {
        return this == COLD || this == CHILLY || this == MILD;
    }

    public SeasonType toSeasonType() {
        return switch (this) {
            case COLD, CHILLY -> SeasonType.COLD;
            case MILD         -> SeasonType.MILD;
            case WARM, HOT    -> SeasonType.HOT;
        };
    }
}