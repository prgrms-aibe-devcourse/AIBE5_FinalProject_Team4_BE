package com.closetnangam.be.domain.clothes.enums;

import com.closetnangam.be.domain.user.entity.User;
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

    /**
     * 가입·프로필 사용자 성별을 옷 분류 gender code로 변환합니다.
     * {@link User.Gender#OTHER} 또는 null은 {@link #UNISEX}로 매핑합니다.
     */
    public static ClothesGender fromUserGender(User.Gender userGender) {
        if (userGender == null || userGender == User.Gender.OTHER) {
            return UNISEX;
        }
        return valueOf(userGender.name());
    }
}
