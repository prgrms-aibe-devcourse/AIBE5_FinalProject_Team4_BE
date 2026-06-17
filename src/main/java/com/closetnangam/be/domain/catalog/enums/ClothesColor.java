package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public static ClothesColor fromCode(String code) {
        if (code == null || code.isBlank()) return WHITE;
        try {
            return ClothesColor.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // 한글이나 알 수 없는 코드는 기본값으로 처리
            return WHITE;
        }
    }
}
