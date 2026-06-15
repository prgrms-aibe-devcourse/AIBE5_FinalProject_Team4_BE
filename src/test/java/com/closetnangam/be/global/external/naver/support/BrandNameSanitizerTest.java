package com.closetnangam.be.global.external.naver.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BrandNameSanitizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "남성청반바지",
            "남자청반바지",
            "정석와이드핏",
            "남자신발",
            "바람막이",
            "기모",
            "신발",
            "와이드핏",
            "레디핏",
            "아웃도어",
            "양털체크바지",
            "경량",
            "정장",
            "스판",
            "체크",
            "윈드브레이커",
            "경량발편한로퍼",
            "밴딩와이드치마바지",
            "밀리터리",
            "워커",
            "블로퍼",
            "슬립온",
            "골지",
            "화이트빌딩",
            "오피스룩",
            "트레이닝",
            "트레이닝바지",
            "바지끈",
            "바지 끈"
    })
    @DisplayName("상품 설명성 토큰은 UNKNOWN으로 정규화한다")
    void sanitizeRejectsProductDescriptors(String brandName) {
        assertThat(BrandNameSanitizer.sanitize(brandName)).isEqualTo("UNKNOWN");
        assertThat(BrandNameSanitizer.isLikelyProductDescriptor(brandName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "폴햄",
            "나이키",
            "더로우",
            "다이나핏",
            "핏플랍",
            "미니멈",
            "ZOOC",
            "오프화이트",
            "블랙야크"
    })
    @DisplayName("실제 브랜드명은 유지한다")
    void sanitizeKeepsRealBrands(String brandName) {
        assertThat(BrandNameSanitizer.sanitize(brandName)).isEqualTo(brandName);
        assertThat(BrandNameSanitizer.isLikelyProductDescriptor(brandName)).isFalse();
    }

    @Test
    @DisplayName("빈 값과 UNKNOWN은 UNKNOWN을 반환한다")
    void sanitizeHandlesBlankAndUnknown() {
        assertThat(BrandNameSanitizer.sanitize(null)).isEqualTo("UNKNOWN");
        assertThat(BrandNameSanitizer.sanitize("")).isEqualTo("UNKNOWN");
        assertThat(BrandNameSanitizer.sanitize("UNKNOWN")).isEqualTo("UNKNOWN");
    }
}
