package com.closetnangam.be.global.external.naver.support;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 와이드 팬츠·와이드 바지류 하의는 남성 키워드가 없으면 여성(FEMALE)으로 분류한다.
 */
public final class WidePantsGenderCorrector {

    private static final String BOTTOM = "BOTTOM";
    private static final String FEMALE = "FEMALE";

    private static final Pattern WIDE_PANTS = Pattern.compile(
            "와이드.*(?:팬츠|바지|슬랙|데님|청)|와이드\\s*(?:팬츠|바지|슬랙|데님|청)|와이드(?:팬츠|바지)|"
                    + "wide\\s*(?:pants|pant|jeans|trousers|slacks)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MALE_KEYWORD = Pattern.compile(
            "남성|남자|남성용|남자용|남녀공용|남여공용|"
                    + "\\b(?:men(?:'s|s)?|man|boys?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private WidePantsGenderCorrector() {
    }

    /**
     * 와이드 팬츠 하의는 남성 키워드가 없으면 FEMALE, 있으면 원래 gender를 유지한다.
     */
    public static String correctGender(String genderCode, String categoryCode, String brandName, String productName) {
        if (!BOTTOM.equalsIgnoreCase(categoryCode != null ? categoryCode.trim() : "")) {
            return genderCode;
        }
        if (!isWidePants(productName)) {
            return genderCode;
        }
        if (hasMaleGenderKeyword(productName)) {
            return genderCode;
        }
        return FEMALE;
    }

    /** DB 정리용 — FEMALE이 아닌 와이드 팬츠 중 보정 대상 여부 */
    public static boolean shouldCorrectToFemale(String genderCode, String categoryCode, String brandName, String productName) {
        if (FEMALE.equalsIgnoreCase(genderCode != null ? genderCode.trim() : "")) {
            return false;
        }
        return FEMALE.equalsIgnoreCase(correctGender(genderCode, categoryCode, brandName, productName));
    }

    public static boolean isWidePants(String productName) {
        return StringUtils.hasText(productName) && WIDE_PANTS.matcher(productName).find();
    }

    private static boolean hasMaleGenderKeyword(String productName) {
        return StringUtils.hasText(productName) && MALE_KEYWORD.matcher(productName).find();
    }
}
