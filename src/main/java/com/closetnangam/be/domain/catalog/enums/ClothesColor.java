package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ClothesColor {

    PINK("핑크", "#FFB6C1"),
    RED("레드", "#E53935"),
    ORANGE("오렌지", "#FF9800"),
    BEIGE("베이지", "#D2B48C"),
    YELLOW("옐로우", "#FDD835"),
    GREEN("그린", "#43A047"),
    LIGHT_BLUE("라이트블루", "#81D4FA"),
    NAVY("네이비", "#1F3A5F"),
    PURPLE("퍼플", "#8E24AA"),
    BROWN("브라운", "#795548"),
    GRAY("그레이", "#9E9E9E"),
    WHITE("화이트", "#FFFFFF"),
    BLACK("블랙", "#212121");

    private final String label;
    private final String hex;

    /** AI·외부 연동에서 자주 쓰이지만 카탈로그 enum에 없는 색상 코드 별칭 */
    private static final Map<String, String> CATALOG_ALIASES = Map.ofEntries(
            Map.entry("BLUE", "LIGHT_BLUE"),
            Map.entry("SKY_BLUE", "LIGHT_BLUE"),
            Map.entry("SKYBLUE", "LIGHT_BLUE"),
            Map.entry("LIGHTBLUE", "LIGHT_BLUE"),
            Map.entry("PASTEL_BLUE", "LIGHT_BLUE"),
            Map.entry("DENIM", "LIGHT_BLUE"),
            Map.entry("DARK_BLUE", "NAVY"),
            Map.entry("DEEP_BLUE", "NAVY"),
            Map.entry("DARKBLUE", "NAVY"),
            Map.entry("MIDNIGHT", "NAVY"),
            Map.entry("INDIGO", "NAVY")
    );

    // 기존 유지
    public static ClothesColor fromCode(String code) {
        return ClothesColor.valueOf(code);
    }

    /**
     * AI 추출·외부 입력 색상 코드를 카탈로그 enum name으로 정규화합니다.
     *
     * @return 매칭되는 카탈로그 코드, 없으면 {@code null}
     */
    public static String normalizeCatalogCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        for (ClothesColor color : values()) {
            if (color.name().equals(normalized)) {
                return color.name();
            }
        }
        String alias = CATALOG_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }
        String trimmed = code.trim();
        for (ClothesColor color : values()) {
            if (color.getLabel().equals(trimmed)) {
                return color.name();
            }
        }
        return null;
    }

    // 표시용 fallback 추가
    public static ClothesColor fromCodeOrDefault(String code) {
        if (code == null || code.isBlank()) return WHITE;
        // 영문 코드 시도
        try {
            return ClothesColor.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {}
        // 한글 label로 역매핑 시도
        for (ClothesColor color : values()) {
            if (color.getLabel().equals(code)) return color;
        }
        return WHITE;
    }
}
