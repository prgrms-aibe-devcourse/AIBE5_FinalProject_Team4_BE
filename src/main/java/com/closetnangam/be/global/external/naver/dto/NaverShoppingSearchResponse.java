package com.closetnangam.be.global.external.naver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 네이버 쇼핑 검색 API 원본 응답을 매핑하기 위한 DTO.
 *
 * 외부 API 응답은 서비스 요구사항보다 많은 필드를 포함할 수 있으므로 ignoreUnknown을 켜 둔다.
 * 이 타입은 내부 도메인 응답이 아니라 NaverApiService가 원본 JSON을 안전하게 파싱하기 위한 경계 모델이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverShoppingSearchResponse(
        Integer total,
        Integer start,
        Integer display,
        List<NaverShoppingItem> items
) {

    /**
     * 네이버 쇼핑 API의 items 배열 원소.
     *
     * lprice/hprice는 네이버 원본 스펙상 문자열로 내려오므로 여기서는 String으로 받고,
     * 프론트 응답으로 바꿀 때 Integer로 변환한다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverShoppingItem(
            String title,
            String link,
            String image,
            String lprice,
            String hprice,
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
    }
}
