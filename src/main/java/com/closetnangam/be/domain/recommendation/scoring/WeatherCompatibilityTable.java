package com.closetnangam.be.domain.recommendation.scoring;


import com.closetnangam.be.global.external.weather.Enum.TemperatureRange;

public final class WeatherCompatibilityTable {

    private WeatherCompatibilityTable() {}

    public static double getWeatherScore(double temp, String itemType) {
        if (itemType == null) return 0.5;

        TemperatureRange range = TemperatureRange.from(temp);

        return switch (range) {
            case HOT  -> scoreForHot(itemType);
            case WARM -> scoreForWarm(itemType);
            case MILD -> scoreForMild(itemType);
            case CHILLY -> scoreForChilly(itemType);
            case COLD -> scoreForCold(itemType);
        };
    }

    // 28°C 이상 — 반팔, 민소매, 반바지
    private static double scoreForHot(String itemType) {
        return switch (itemType) {
            case "SHORT_SLEEVE", "SLEEVELESS", "SHORTS" -> 1.0;
            case "COLLAR_TEE", "COTTON"                 -> 0.8;
            case "DENIM", "SKIRT", "CARGO"              -> 0.6;
            case "LONG_SLEEVE", "KNIT", "SWEAT"        -> 0.2;
            case "HOODIE", "TRAINING_JACKET",
                 "FLEECE_JACKET", "PADDING",
                 "SINGLE_COAT", "DOUBLE_COAT"           -> 0.0;
            default                                     -> 0.4;
        };
    }

    // 20~27°C — 반팔, 얇은 셔츠
    private static double scoreForWarm(String itemType) {
        return switch (itemType) {
            case "SHORT_SLEEVE", "SHIRT", "COLLAR_TEE" -> 1.0;
            case "LONG_SLEEVE", "COTTON", "DENIM",
                 "SKIRT", "SHORTS"                     -> 0.8;
            case "SWEAT", "HOODIE", "CARGO"            -> 0.6;
            case "BLOUSON", "DENIM_JACKET", "VEST"     -> 0.5;
            case "KNIT", "FLEECE_JACKET"               -> 0.3;
            case "PADDING", "SINGLE_COAT",
                 "DOUBLE_COAT", "BALMACAAN_COAT"        -> 0.1;
            default                                     -> 0.5;
        };
    }

    // 12~19°C — 가디건, 가벼운 자켓
    private static double scoreForMild(String itemType) {
        return switch (itemType) {
            case "LONG_SLEEVE", "KNIT", "SHIRT"        -> 1.0;
            case "DENIM_JACKET", "BLOUSON", "BLAZER",
                 "VEST", "MA1"                         -> 0.9;
            case "DENIM", "SLACKS", "COTTON", "CARGO"  -> 0.8;
            case "SWEAT", "HOODIE"                     -> 0.7;
            case "SHORT_SLEEVE", "COLLAR_TEE"          -> 0.5;
            case "FLEECE_JACKET", "LIGHT_PADDING"      -> 0.6;
            case "PADDING", "SINGLE_COAT"              -> 0.3;
            case "SLEEVELESS", "SHORTS"                -> 0.2;
            default                                     -> 0.5;
        };
    }

    // 5~11°C — 두꺼운 아우터
    private static double scoreForChilly(String itemType) {
        return switch (itemType) {
            case "KNIT", "HOODIE", "SWEAT"             -> 1.0;
            case "SINGLE_COAT", "BALMACAAN_COAT",
                 "DOUBLE_COAT", "SHEARLING"            -> 0.9;
            case "FLEECE_JACKET", "LIGHT_PADDING",
                 "LEATHER_JACKET"                      -> 0.8;
            case "LONG_SLEEVE", "SHIRT"                -> 0.7;
            case "DENIM", "SLACKS", "CARGO"            -> 0.7;
            case "PADDING"                             -> 0.6;
            case "SHORT_SLEEVE", "COLLAR_TEE"          -> 0.3;
            case "SLEEVELESS", "SHORTS"                -> 0.0;
            default                                     -> 0.5;
        };
    }

    // ~4°C — 패딩, 두꺼운 코트
    private static double scoreForCold(String itemType) {
        return switch (itemType) {
            case "PADDING", "SINGLE_COAT",
                 "DOUBLE_COAT", "BALMACAAN_COAT",
                 "TTEOKBOKKI_COAT", "SHEARLING"        -> 1.0;
            case "FLEECE_JACKET", "LIGHT_PADDING"      -> 0.8;
            case "KNIT", "HOODIE"                      -> 0.7;
            case "BOOTS"                               -> 0.9;
            case "DENIM", "SLACKS", "TRAINING"         -> 0.6;
            case "SWEAT", "LONG_SLEEVE"                -> 0.5;
            case "SHORT_SLEEVE", "COLLAR_TEE"          -> 0.1;
            case "SLEEVELESS", "SHORTS", "SKIRT"       -> 0.0;
            default                                     -> 0.4;
        };
    }
}