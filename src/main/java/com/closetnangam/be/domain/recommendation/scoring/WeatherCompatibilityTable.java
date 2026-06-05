package com.closetnangam.be.domain.recommendation.scoring;

import com.closetnangam.be.domain.clothes.enums.TemperatureRange;

public class WeatherCompatibilityTable {

    /**
     * 기온과 아이템 타입에 따른 적합도 점수(0.0 ~ 1.0)를 반환합니다.
     */
    public static double getWeatherScore(double temp, String itemType) {
        TemperatureRange range = TemperatureRange.from(temp);
        
        if (itemType == null) return 0.5;
        String type = itemType.toUpperCase();

        return switch (range) {
            case HOT -> scoreForHot(type);
            case WARM -> scoreForWarm(type);
            case MILD -> scoreForMild(type);
            case COLD -> scoreForCold(type);
        };
    }

    private static double scoreForHot(String type) {
        if (type.contains("SHORT") || type.contains("HALF") || type.contains("SLEEVELESS") || type.contains("SHORTS")) return 1.0;
        if (type.contains("OUTER") || type.contains("KNIT") || type.contains("LONG")) return 0.2;
        return 0.5;
    }

    private static double scoreForWarm(String type) {
        if (type.contains("SHORT") || type.contains("SHIRT") || type.contains("PANTS")) return 1.0;
        if (type.contains("HEAVY") || type.contains("PADDING")) return 0.1;
        return 0.7;
    }

    private static double scoreForMild(String type) {
        if (type.contains("LONG") || type.contains("KNIT") || type.contains("JACKET") || type.contains("CARDIGAN")) return 1.0;
        if (type.contains("SLEEVELESS") || type.contains("SHORTS")) return 0.3;
        return 0.8;
    }

    private static double scoreForCold(String type) {
        if (type.contains("HEAVY") || type.contains("PADDING") || type.contains("COAT") || type.contains("KNIT")) return 1.0;
        if (type.contains("SHORT") || type.contains("SHORTS") || type.contains("SLEEVELESS")) return 0.0;
        return 0.6;
    }
}
