package com.closetnangam.be.domain.recommendation.controller;

import com.closetnangam.be.domain.recommendation.dto.response.OotdResponse;
import com.closetnangam.be.domain.recommendation.service.OotdRecommendationService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OOTD 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ootd")
public class OotdController {

    private final OotdRecommendationService ootdRecommendationService;

    @Operation(summary = "내 옷장 OOTD 추천", description = "내 옷장의 보유 의상을 바탕으로 오늘 기온에 맞는 상의+하의(+아우터) 조합을 추천합니다.")
    @GetMapping("/{wardrobeId}")
    public ResponseEntity<ApiResponse<OotdResponse>> getOotd(
            @PathVariable Long wardrobeId,
            @RequestParam double currentTemp
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        OotdResponse response = ootdRecommendationService.recommend(currentUserId, wardrobeId, currentTemp);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
