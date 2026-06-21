package com.closetnangam.be.global.external.naver.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BrandGenderCorrectorTest {

    @ParameterizedTest(name = "brand={2} -> FEMALE")
    @CsvSource({
            "MALE, TOP, 올리브데올리브",
            "UNISEX, BOTTOM, OLIVE DES OLIVE",
            "MALE, OUTER, olivedesolive",
            "MALE, TOP, JJ지고트",
            "UNISEX, BOTTOM, JJ JIGOTT",
            "MALE, OUTER, BCBG",
            "MALE, TOP, 플라스틱 아일랜드",
            "UNISEX, BOTTOM, PLASTIC ISLAND",
            "MALE, TOP, SOUP",
            "UNISEX, BOTTOM, 숲",
            "MALE, TOP, 제시뉴욕",
            "UNISEX, BOTTOM, JESSI NEW YORK",
            "MALE, TOP, 로엠",
            "UNISEX, BOTTOM, TRIANA",
            "MALE, TOP, 미니멈",
            "UNISEX, BOTTOM, EGOIST",
            "MALE, TOP, ear papillonner",
            "UNISEX, BOTTOM, 마담엘레강스",
            "MALE, TOP, 몰리올리",
            "UNISEX, BOTTOM, LIST",
            "MALE, TOP, 쉬즈미스",
    })
    @DisplayName("여성 전용 브랜드는 FEMALE로 보정한다")
    void correctGenderToFemale(String gender, String category, String brand) {
        assertThat(BrandGenderCorrector.correctGender(gender, brand)).isEqualTo("FEMALE");
    }

    @ParameterizedTest(name = "brand={1} -> unchanged")
    @CsvSource({
            "MALE, 폴햄",
            "UNISEX, NIKE",
    })
    @DisplayName("일반 브랜드는 gender를 유지한다")
    void keepOriginalGender(String gender, String brand) {
        assertThat(BrandGenderCorrector.correctGender(gender, brand)).isEqualTo(gender);
    }
}
