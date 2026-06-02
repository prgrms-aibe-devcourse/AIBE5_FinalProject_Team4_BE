package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;

import java.util.List;

/**
 * 유사 상품 추천 화면에 필요한 데이터를 한 번에 내려주기 위한 응답 DTO.
 *
 * @param baseClothes 사용자가 모달에서 선택한 기준 옷. 프론트에서 "이 옷과 비슷한 상품" 영역에 표시한다.
 * @param query 네이버 쇼핑 API에 실제로 전달한 검색어. 검색 품질 확인과 디버깅에 사용한다.
 * @param products 네이버 쇼핑 검색 결과를 서비스 내부 형식으로 정리한 추천 상품 목록.
 */
public record SimilarProductRecommendationResponse(
        ClothesResponse baseClothes,
        String query,
        List<NaverShoppingProductResponse> products
) {
}
