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
    @DisplayName("뒤쪽 혼합 영숫자 품번(PHE2PT2750 등)을 제거한다")
    void removesTrailingMixedAlphanumericSku() {
        NaverShoppingTitleSanitizer.Result polham = NaverShoppingTitleSanitizer.sanitize(
                "폴햄 알래스카 크롭 와이드 팬츠 PHE2PT2750",
                "56199141706"
        );
        assertThat(polham.displayName()).isEqualTo("폴햄 알래스카 크롭 와이드 팬츠");
        assertThat(polham.productCode()).isEqualTo("PHE2PT2750");

        NaverShoppingTitleSanitizer.Result spao = NaverShoppingTitleSanitizer.sanitize(
                "SPAO 벌룬 와이드 팬츠 나일론 SPTCG25G12",
                "60279832174"
        );
        assertThat(spao.displayName()).isEqualTo("SPAO 벌룬 와이드 팬츠 나일론");
        assertThat(spao.productCode()).isEqualTo("SPTCG25G12");
    }

    @Test
    @DisplayName("신발 모델명(990V6, GT2160)은 제거하지 않는다")
    void preservesProductModelNames() {
        NaverShoppingTitleSanitizer.Result newBalance = NaverShoppingTitleSanitizer.sanitize(
                "뉴발란스 990V6 러닝화",
                "60000000001"
        );
        assertThat(newBalance.displayName()).isEqualTo("뉴발란스 990V6 러닝화");
        assertThat(newBalance.productCode()).isEqualTo("NAVER_60000000001");

        NaverShoppingTitleSanitizer.Result asics = NaverShoppingTitleSanitizer.sanitize(
                "아식스 젤 GT2160 운동화",
                "60000000002"
        );
        assertThat(asics.displayName()).isEqualTo("아식스 젤 GT2160 운동화");
        assertThat(asics.productCode()).isEqualTo("NAVER_60000000002");
    }

    @Test
    @DisplayName("모델명 뒤에 내부 SKU가 있으면 SKU만 제거한다")
    void removesSkuAfterModelName() {
        NaverShoppingTitleSanitizer.Result result = NaverShoppingTitleSanitizer.sanitize(
                "뉴발란스 990V6 PHE2PT2750",
                "60000000003"
        );

        assertThat(result.displayName()).isEqualTo("뉴발란스 990V6");
        assertThat(result.productCode()).isEqualTo("PHE2PT2750");
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
