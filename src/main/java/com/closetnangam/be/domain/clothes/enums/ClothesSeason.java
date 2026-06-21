package com.closetnangam.be.domain.clothes.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum ClothesSeason {

    SPRING("봄"),
    SUMMER("여름"),
    FALL("가을"),
    WINTER("겨울"),
    ALL_SEASON("사계절");

    private final String label;

    public static ClothesSeason fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("시즌 코드는 필수입니다.");
        }
        return fromNormalized(code.trim().toUpperCase());
    }

    public static ClothesSeason fromCodeOrDefault(String code) {
        if (!StringUtils.hasText(code)) {
            return ALL_SEASON;
        }
        try {
            return fromNormalized(code.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ALL_SEASON;
        }
    }

    public static ClothesSeason fromTemperature(double temp) {
        if (temp < 5.0) return WINTER;
        if (temp < 20.0) return FALL;
        if (temp < 24.0) return SPRING;
        return SUMMER;
    }

    public boolean isCompatibleWith(ClothesSeason other) {
        if (this == ALL_SEASON || other == ALL_SEASON) return true;
        if (this == other) return true;
        // 봄-가을은 서로 호환된다고 가정 (보통 춘추복)
        if ((this == SPRING && other == FALL) || (this == FALL && other == SPRING)) return true;
        return false;
    }

    public boolean requiresOuter() {
        return this == WINTER || this == FALL;
    }

    private static ClothesSeason fromNormalized(String normalized) {
        if ("AUTUMN".equals(normalized)) {
            return FALL;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("유효하지 않은 시즌 코드입니다: " + normalized);
        }
    }
}
