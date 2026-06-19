package com.closetnangam.be.global.external.naver.dto;

/**
 * 프론트에 내려주는 네이버 쇼핑 상품 응답 DTO.
 *
 * 네이버 원본 응답은 가격이 문자열이고 title에 HTML 태그가 섞일 수 있다.
 * 이 DTO는 서비스에서 한 번 정제한 뒤 카드 UI에 바로 쓰기 쉬운 형태로 내려주기 위한 모델이다.
 */
public record NaverShoppingProductResponse(
        String title,
        String link,
        String image,
        Integer lowestPrice,
        Integer highestPrice,
        String mallName,
        String productId,
        String productType,
        String brand,
        String maker,
        String category1,
        String category2,
        String category3,
        String category4,
        Long clothesId,
        String candidateSource,
        String primaryColor,
        String primaryStyle
) {
    public NaverShoppingProductResponse(
            String title,
            String link,
            String image,
            Integer lowestPrice,
            Integer highestPrice,
            String mallName,
            String productId,
            String productType,
            String brand,
            String maker,
            String category1,
            String category2,
            String category3,
            String category4,
            Long clothesId,
            String candidateSource
    ) {
        this(
                title,
                link,
                image,
                lowestPrice,
                highestPrice,
                mallName,
                productId,
                productType,
                brand,
                maker,
                category1,
                category2,
                category3,
                category4,
                clothesId,
                candidateSource,
                null,
                null
        );
    }

    public NaverShoppingProductResponse(
            String title,
            String link,
            String image,
            Integer lowestPrice,
            Integer highestPrice,
            String mallName,
            String productId,
            String productType,
            String brand,
            String maker,
            String category1,
            String category2,
            String category3,
            String category4
    ) {
        this(
                title,
                link,
                image,
                lowestPrice,
                highestPrice,
                mallName,
                productId,
                productType,
                brand,
                maker,
                category1,
                category2,
                category3,
                category4,
                null,
                "NAVER",
                null,
                null
        );
    }
}
