package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StyleCode {

    CASUAL("캐주얼", "편안하고 일상적인 스타일"),
    STREET("스트릿", "스트릿 패션 중심의 스타일"),
    MINIMAL("미니멀", "단순하고 깔끔한 스타일"),
    SPORTY("스포티", "스포츠웨어 기반의 활동적인 스타일"),
    CLASSIC("클래식", "전통적이고 정돈된 스타일"),
    CHIC("시크", "세련되고 도시적인 스타일"),
    WORKWEAR("워크웨어", "작업복·유틸리티 중심의 스타일"),
    CITYBOY("시티보이", "도심형 캐주얼 스타일"),
    GORPCORE("고프코어", "아웃도어·기능성 중심의 스타일"),
    RETRO("레트로", "복고풍을 연상시키는 스타일");

    private final String label;
    private final String description;

    public static StyleCode fromCode(String code) {
        if (code == null || code.isBlank()) return CASUAL;
        try {
            return StyleCode.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return CASUAL;
        }
    }
}
