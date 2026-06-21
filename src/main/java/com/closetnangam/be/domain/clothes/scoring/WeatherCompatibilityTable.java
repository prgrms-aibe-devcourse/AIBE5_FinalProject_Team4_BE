package com.closetnangam.be.domain.clothes.scoring;

import com.closetnangam.be.domain.clothes.enums.ClothesSeason;

public class WeatherCompatibilityTable {

    /**
     * 기온과 아이템 타입에 따른 적합도 점수(0.0 ~ 1.0)를 반환합니다.
     */
    public static double getWeatherScore(double temp, String itemType) {
        ClothesSeason season = ClothesSeason.fromTemperature(temp);
        if (itemType == null) return 0.5;
        String type = itemType.toUpperCase();

        return switch (season) {
            case SUMMER -> scoreForHot(type);
            case SPRING, FALL -> scoreForMild(type); // 봄/가을 통합
            case WINTER -> scoreForCold(type);
            default -> 0.5;
        };
    }

    private static double scoreForHot(String type) {
        return switch (type) {
            case "SHORT_SLEEVE", "SLEEVELESS", "SHORTS", "SANDALS_SLIPPERS" -> 1.0;
            case "COLLAR_TEE", "SKIRT" -> 0.9;
            case "FLAT", "LOAFER" -> 0.9;
            case "BOOTS", "ANKLE_BOOTS" -> 0.2;
            case "SHIRT", "COTTON", "DENIM" -> 0.6;
            case "LONG_SLEEVE", "KNIT", "SWEAT", "HOODIE", "SLACKS" -> 0.2;
            case "PADDING", "COAT", "HEAVY", "LEATHER_JACKET", "CARDIGAN", "JACKET", "BLAZER", "WINDBREAKER", "HOOD_ZIPUP", "DENIM_JACKET", "VARSITY_JACKET", "WORK_JACKET", "MA1", "BLOUSON" -> 0.0;
            default -> 0.3;
        };
    }

    private static double scoreForWarm(String type) {
        return switch (type) {
            case "SHORT_SLEEVE", "SHIRT", "COLLAR_TEE", "COTTON", "DENIM", "SLACKS", "SKIRT" -> 1.0;
            case "LONG_SLEEVE", "SWEAT", "HOODIE", "CARGO" -> 0.8;
            case "WINDBREAKER", "HOOD_ZIPUP", "BLAZER" -> 0.6;
            case "KNIT", "DENIM_JACKET", "VARSITY_JACKET" -> 0.4;
            case "PADDING", "COAT" -> 0.0;
            default -> 0.7;
        };
    }

    private static double scoreForMild(String type) {
        return switch (type) {
            case "LONG_SLEEVE", "KNIT", "SHIRT", "SWEAT", "HOODIE" -> 1.0;
            case "DENIM_JACKET", "BLOUSON", "BLAZER", "VEST", "MA1", "WINDBREAKER", "HOOD_ZIPUP", "VARSITY_JACKET", "WORK_JACKET" -> 0.9;
            case "BOOTS", "ANKLE_BOOTS" -> 0.9;
            case "DENIM", "SLACKS", "COTTON", "CARGO", "SKIRT" -> 0.8;
            case "FLAT", "LOAFER", "SNEAKERS" -> 0.8;
            case "SLEEVELESS", "SHORTS" -> 0.2;
            case "PADDING", "HEAVY" -> 0.1;
            default -> 0.5;
        };
    }

    private static double scoreForChilly(String type) {
        return switch (type) {
            case "KNIT", "SWEAT", "HOODIE", "SINGLE_COAT", "BALMACAAN_COAT", "DENIM_JACKET", "BLOUSON", "MA1", "VARSITY_JACKET", "LIGHT_PADDING" -> 1.0;
            case "LONG_SLEEVE", "SHIRT", "VEST", "LEATHER_JACKET", "WORK_JACKET" -> 0.8;
            case "DENIM", "SLACKS", "COTTON", "CARGO" -> 0.8;
            case "SHORT_SLEEVE", "SHORTS", "SLEEVELESS" -> 0.0;
            default -> 0.6;
        };
    }

    private static double scoreForCold(String type) {
        return switch (type) {
            case "PADDING", "HEAVY", "SHEARLING", "DOUBLE_COAT", "TTEOKBOKKI_COAT", "KNIT", "FLEECE_JACKET" -> 1.0;
            case "BOOTS", "ANKLE_BOOTS" -> 1.0;
            case "SINGLE_COAT", "BALMACAAN_COAT", "MA1", "SWEAT", "HOODIE" -> 0.7;
            case "DENIM", "SLACKS", "COTTON" -> 0.6;
            case "FLAT", "LOAFER" -> 0.5;
            case "SHORT_SLEEVE", "SHORTS", "SLEEVELESS", "SANDALS_SLIPPERS" -> 0.0;
            default -> 0.5;
        };
    }
}
