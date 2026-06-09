package com.closetnangam.be.domain.recommendation.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplementaryRecommendationGeminiPromptsTest {

    @Test
    @DisplayName("쇼핑 상품 분류 프롬프트에 메타정보와 가이드를 포함한다")
    void buildShoppingProductPromptIncludesMetadata() {
        String prompt = ComplementaryRecommendationGeminiPrompts.buildShoppingProductPrompt(
                "[category]\n- TOP",
                "코튼 티셔츠",
                "브랜드A",
                "남성의류"
        );

        assertThat(prompt).contains("코튼 티셔츠");
        assertThat(prompt).contains("브랜드A");
        assertThat(prompt).contains("남성의류");
        assertThat(prompt).contains("[category]");
        assertThat(prompt).contains("비의류");
    }
}
