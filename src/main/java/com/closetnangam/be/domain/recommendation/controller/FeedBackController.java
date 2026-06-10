package com.closetnangam.be.domain.recommendation.controller;

import com.closetnangam.be.domain.recommendation.dto.request.RecommendationFeedbackRequest;
import com.closetnangam.be.domain.recommendation.service.RecommendationFeedbackService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FeedBackController {
    private final RecommendationFeedbackService recommendationFeedbackService;

    @Operation(
            summary = "추천 상품 피드백",
            description = "추천받은 상품에 대해 저장하기, 싫어요 또는 추천 제외 피드백을 남깁니다."
    )
    @PostMapping("/users/{userId}/recommendations/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long userId,
            @Valid @RequestBody RecommendationFeedbackRequest request
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        recommendationFeedbackService.submitFeedback(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
