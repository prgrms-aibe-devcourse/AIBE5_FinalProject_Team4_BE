package com.closetnangam.be.global.external.weather.Enum;

public enum TemperatureRange {

    COLD,    // ~4°C   겨울
    CHILLY,  // 5~11°C 초봄/늦가을
    MILD,    // 12~19°C 봄/가을
    WARM,    // 20~27°C 초여름
    HOT;     // 28°C~  여름

    public static TemperatureRange from(double currentTemp) {
        if (currentTemp < 5.0)  return COLD;
        if (currentTemp < 12.0) return CHILLY;
        if (currentTemp < 20.0) return MILD;
        if (currentTemp < 28.0) return WARM;
        return HOT;
    }

    public SeasonType toSeasonType() {
        return switch (this) {
            case COLD, CHILLY -> SeasonType.COLD;
            case MILD         -> SeasonType.MILD;
            case WARM, HOT    -> SeasonType.HOT;
        };
    }
}