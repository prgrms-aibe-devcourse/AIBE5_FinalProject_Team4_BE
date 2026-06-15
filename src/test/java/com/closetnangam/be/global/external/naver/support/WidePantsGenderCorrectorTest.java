package com.closetnangam.be.global.external.naver.support;



import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.CsvSource;



import static org.assertj.core.api.Assertions.assertThat;



class WidePantsGenderCorrectorTest {



    @ParameterizedTest

    @CsvSource({

            "MALE, BOTTOM, 쉬즈미스, 쉬즈미스 와이드 밴딩 팬츠, FEMALE",

            "MALE, BOTTOM, 리스트, 리스트 투턱 와이드 슬랙스, FEMALE",

            "MALE, BOTTOM, UNKNOWN, 와이드 팬츠, FEMALE",

            "MALE, BOTTOM, UNKNOWN, 와이드 바지, FEMALE",

            "UNISEX, BOTTOM, UNKNOWN, 와이드 팬츠, FEMALE",

            "MALE, BOTTOM, 폴햄, 폴햄 남성 와이드 데님, MALE",

            "MALE, BOTTOM, UNKNOWN, 남자 빅사이즈 와이드 팬츠, MALE",

            "MALE, BOTTOM, 폴햄, 폴햄 와이드 팬츠, FEMALE",

            "MALE, TOP, 쉬즈미스, 쉬즈미스 와이드 팬츠, MALE",

            "FEMALE, BOTTOM, 폴햄, 폴햄 와이드 팬츠, FEMALE"

    })

    @DisplayName("와이드 팬츠·와이드 바지는 남성 키워드가 없으면 FEMALE로 분류한다")

    void correctGender(String gender, String category, String brand, String name, String expected) {

        assertThat(WidePantsGenderCorrector.correctGender(gender, category, brand, name)).isEqualTo(expected);

    }



    @Test

    @DisplayName("와이드 팬츠가 아니면 gender를 변경하지 않는다")

    void doesNotChangeNonWidePants() {

        assertThat(WidePantsGenderCorrector.correctGender(

                "MALE",

                "BOTTOM",

                "폴햄",

                "폴햄 슬림 데님"

        )).isEqualTo("MALE");

    }

}


