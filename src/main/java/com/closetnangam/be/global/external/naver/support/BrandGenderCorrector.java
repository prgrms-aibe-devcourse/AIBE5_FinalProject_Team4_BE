package com.closetnangam.be.global.external.naver.support;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 브랜드 특성상 성별이 고정된 경우 AI/규칙 분류 결과를 보정한다.
 */
public final class BrandGenderCorrector {

    private static final String FEMALE = "FEMALE";

    private static final Set<String> FEMALE_BRANDS = Set.of(
            "OLIVEDESOLIVE",
            "OLIVEDEOLIVE",
            "OLIVE DES OLIVE",
            "OLIVE DE OLIVE",
            "올리브데올리브",
            "JJJIGOTT",
            "JJ JIGOTT",
            "JJ지고트",
            "JIGOTT",
            "BCBG",
            "BCBGMAXAZRIA",
            "BCBG MAXAZRIA",
            "PLASTICISLAND",
            "PLASTIC ISLAND",
            "플라스틱아일랜드",
            "플라스틱 아일랜드",
            "SOUP",
            "숲"
    );

    private BrandGenderCorrector() {
    }

    public static String correctGender(String genderCode, String brandName) {
        if (isFemaleBrand(brandName)) {
            return FEMALE;
        }
        return genderCode;
    }

    static boolean isFemaleBrand(String brandName) {
        if (!StringUtils.hasText(brandName)) {
            return false;
        }
        String trimmed = brandName.trim();
        return FEMALE_BRANDS.contains(trimmed)
                || FEMALE_BRANDS.contains(trimmed.toUpperCase(Locale.ROOT))
                || FEMALE_BRANDS.contains(normalizeKey(trimmed));
    }

    private static String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s.'\\-&]", "");
    }
}
