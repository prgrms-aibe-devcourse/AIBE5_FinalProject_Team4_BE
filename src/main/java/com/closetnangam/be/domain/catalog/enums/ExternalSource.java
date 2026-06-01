package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ExternalSource {

    NAVER_SHOPPING("네이버쇼핑", ExternalSourceGroup.OPEN_MARKET, false),
    COUPANG("쿠팡", ExternalSourceGroup.OPEN_MARKET, false),
    MUSINSA("무신사", ExternalSourceGroup.FASHION_PLATFORM, false),
    ABLY("에이블리", ExternalSourceGroup.FASHION_PLATFORM, false),
    ZIGZAG("지그재그", ExternalSourceGroup.FASHION_PLATFORM, false),
    TWENTYNINE_CM("29CM", ExternalSourceGroup.FASHION_PLATFORM, false),
    WCONCEPT("W컨셉", ExternalSourceGroup.FASHION_PLATFORM, false),
    BRANDI("브랜디", ExternalSourceGroup.FASHION_PLATFORM, false),
    UNIQLO("유니클로", ExternalSourceGroup.SPA, false),
    SPAO("스파오", ExternalSourceGroup.SPA, false),
    EIGHT_SECONDS("에잇세컨즈", ExternalSourceGroup.SPA, false),
    HM("H&M", ExternalSourceGroup.SPA, false),
    CUSTOM("직접입력", ExternalSourceGroup.CUSTOM, true);

    private final String label;
    private final ExternalSourceGroup group;
    private final boolean allowsCustomInput;

    public static ExternalSource fromCode(String code) {
        return Arrays.stream(values())
                .filter(source -> source.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 외부 출처 코드입니다: " + code));
    }

    public static Optional<ExternalSource> findByCode(String code) {
        return Arrays.stream(values())
                .filter(source -> source.name().equals(code))
                .findFirst();
    }

    public static String resolveDisplayName(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        return findByCode(storedValue)
                .map(ExternalSource::getLabel)
                .orElse(storedValue);
    }
}
