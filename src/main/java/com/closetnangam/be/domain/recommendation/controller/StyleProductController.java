package com.closetnangam.be.domain.recommendation.controller;

import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.domain.recommendation.service.StyleProductRecommender;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class StyleProductController {

    private final StyleProductRecommender styleProductRecommender;


    @Operation(
            summary = "취향 기반 상품 추천",
            description = "옷장 내 의상 데이터와 현재 기온을 기반으로 스타일·색상·날씨 적합도를 분석하여 추천 상품 최대 20개를 반환합니다."
    )
    @GetMapping("/{wardrobeId}")
    public ResponseEntity<List<RecommendResponse>> getRecommendations(
            @PathVariable Long wardrobeId,
            @RequestParam(defaultValue = "20.0") double currentTemp) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(styleProductRecommender.recommendByStyle(currentUserId, wardrobeId, currentTemp));
    }
}
