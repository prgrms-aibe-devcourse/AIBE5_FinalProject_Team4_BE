package com.closetnangam.be.global.external.naver.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverShoppingTitleSanitizerTest {

    @Test
    @DisplayName("뒤쪽 알파벳+숫자 품번을 제거한다")
    void removesTrailingAlphanumericSku() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "KHAITE 26 카이트 Bella 레더 앵클 부츠 TP573371404",
                "59879757360"
        );

        assertThat(result.displayName()).isEqualTo("KHAITE 26 카이트 Bella 레더 앵클 부츠");
        assertThat(result.productCode()).isEqualTo("TP573371404");
    }

    @Test
    @DisplayName("뒤쪽 하이픈 SKU를 제거한다")
    void removesTrailingHyphenSku() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "파파브로 남성 기본핏 무지 긴팔 맨투맨 ON-TSA-QWFN0XTH",
                "59576954199"
        );

        assertThat(result.displayName()).isEqualTo("파파브로 남성 기본핏 무지 긴팔 맨투맨");
        assertThat(result.productCode()).isEqualTo("ON-TSA-QWFN0XTH");
    }

    @Test
    @DisplayName("시즌 코드와 품번이 연달아 있으면 모두 제거한다")
    void removesSeasonCodeAndSku() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "A DICIANNOVEVENTITRE 옥타 컴뱃 부츠 SS26 OCTA COMBAT TP866840042",
                "58786323317"
        );

        assertThat(result.displayName()).isEqualTo("A DICIANNOVEVENTITRE 옥타 컴뱃 부츠");
        assertThat(result.productCode()).isEqualTo("TP866840042");
    }

    @Test
    @DisplayName("뒤쪽 사이즈 표기를 제거한다")
    void removesTrailingSizeSuffix() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "베씨 남성 베이직 브이넥 긴팔티셔츠 남자 맨투맨 단체복 브이넥-그레이 M 95",
                "56657952601"
        );

        assertThat(result.displayName()).isEqualTo("베씨 남성 베이직 브이넥 긴팔티셔츠 남자 맨투맨 단체복 브이넥-그레이");
        assertThat(result.productCode()).isEqualTo("NAVER_56657952601");
    }

    @Test
    @DisplayName("끝의 단일 알파벳 코드를 제거한다")
    void removesTrailingSingleLetterCode() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "26 비즈빔 와이퍼 포크 디스트레스드 가죽 레이스업 부츠 T",
                "59461760224"
        );

        assertThat(result.displayName()).isEqualTo("26 비즈빔 와이퍼 포크 디스트레스드 가죽 레이스업 부츠");
        assertThat(result.productCode()).isEqualTo("NAVER_59461760224");
    }

    @Test
    @DisplayName("품번이 없는 제목은 그대로 유지한다")
    void keepsPlainTitle() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "남자 맨투맨 티셔츠 기모 면티 겨울 데일리 코디",
                "49667090868"
        );

        assertThat(result.displayName()).isEqualTo("남자 맨투맨 티셔츠 기모 면티 겨울 데일리 코디");
        assertThat(result.productCode()).isEqualTo("NAVER_49667090868");
    }
}
