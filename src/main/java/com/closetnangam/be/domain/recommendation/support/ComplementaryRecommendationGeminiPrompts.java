package com.closetnangam.be.domain.recommendation.support;

import org.springframework.util.StringUtils;

/**
 * RECO-004 Gemini 분류 프롬프트 (동기 API·Batch API 공용).
 */
public final class ComplementaryRecommendationGeminiPrompts {

    private ComplementaryRecommendationGeminiPrompts() {
    }

    public static String buildShoppingProductPrompt(
            String classificationGuide,
            String productTitle,
            String brandName,
            String shoppingCategory
    ) {
        return """
                당신은 의류 분류 AI입니다. 쇼핑몰 상품 이미지와 아래 메타정보를 함께 보고, 가이드의 코드만 사용해 JSON으로 응답하세요.
                상의·하의·아우터·신발만 분류 대상입니다. 속옷·니시·나시·이너런닝·팬티·브라·1+1·2+1 행사·단체티·주문제작·제작건·단추·부자재·부속품·양말·안경·옷걸이·가방·모자·시계·액세서리 등 비의류·묶음·맞춤제작 상품은 분류하지 마세요. 런닝자켓·런닝복·더블버튼 코트 등 의류는 분류 대상입니다.
                상품명: %s
                브랜드: %s
                쇼핑몰 카테고리: %s

                %s

                반드시 아래 JSON 형식만 반환하세요.
                {
                  "name": "string",
                  "brandName": "string",
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": ["NAVY"],
                  "styles": ["CASUAL"],
                  "gender": "UNISEX",
                  "season": "ALL_SEASON"
                }
                """.formatted(
                defaultText(productTitle),
                defaultText(brandName),
                defaultText(shoppingCategory),
                classificationGuide
        );
    }

    private static String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "UNKNOWN";
    }
}