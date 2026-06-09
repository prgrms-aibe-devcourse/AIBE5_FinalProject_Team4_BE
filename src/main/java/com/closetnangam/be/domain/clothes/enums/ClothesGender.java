package com.closetnangam.be.domain.clothes.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum ClothesGender {

    MALE("남성"),
    FEMALE("여성"),
    UNISEX("유니섹스");

    private final String label;

    public static ClothesGender fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("성별 코드는 필수입니다.");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("유효하지 않은 성별 코드입니다: " + code);
        }
    }

    public static ClothesGender fromCodeOrDefault(String code) {
        if (!StringUtils.hasText(code)) {
            return UNISEX;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UNISEX;
        }
    }
}
