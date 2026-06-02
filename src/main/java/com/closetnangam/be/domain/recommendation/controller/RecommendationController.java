package com.closetnangam.be.domain.recommendation.controller;

import com.closetnangam.be.domain.recommendation.dto.response.SimilarProductRecommendationResponse;
import com.closetnangam.be.domain.recommendation.service.SimilarProductRecommendationService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation", description = "제품 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RecommendationController {

    private final SimilarProductRecommendationService similarProductRecommendationService;

    /*
     * 프론트 흐름:
     * 1. 기존 보유 옷 목록 API로 사용자의 옷을 모달에 보여준다.
     * 2. 사용자가 모달에서 옷 하나를 선택하면 해당 clothesId로 이 API를 호출한다.
     * 3. 응답의 products를 추천 상품 카드 목록으로 렌더링한다.
     *
     * path의 userId는 API 경로 일관성을 위해 유지한다.
     * 단, 추천 기준 옷은 개인 옷장 데이터이므로 JWT의 사용자와 path userId가 같은지 컨트롤러에서 먼저 검증한다.
     */
    @Operation(
            summary = "유사 상품 추천",
            description = "사용자의 보유 옷 하나를 기준으로 네이버 쇼핑에서 유사 상품을 검색해 추천합니다."
    )
    @GetMapping("/users/{userId}/clothes/{clothesId}/similar-products")
    public ResponseEntity<ApiResponse<SimilarProductRecommendationResponse>> getSimilarProducts(
            @PathVariable Long userId,
            @PathVariable Long clothesId
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                similarProductRecommendationService.recommendSimilarProducts(userId, clothesId)
        ));
    }
}
