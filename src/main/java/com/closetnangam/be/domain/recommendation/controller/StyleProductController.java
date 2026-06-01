package com.closetnangam.be.domain.recommendation.controller;

import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;

import com.closetnangam.be.domain.recommendation.service.StyleProductRecommender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class StyleProductController {

    private final StyleProductRecommender styleProductRecommender;

    // 특정 옷장의 스타일 기반 추천 API
    @GetMapping("/{wardrobeId}")
    public ResponseEntity<List<RecommendResponse>> getRecommendations(@PathVariable Long wardrobeId) {
        List<RecommendResponse> recommendations = styleProductRecommender.recommendByStyle(wardrobeId);
        log.info("[Trace] Controller - ResponseEntity 반환 직전 최종 데이터 개수: {}개, wardrobeId={}", recommendations.size(), wardrobeId);
        return ResponseEntity.ok(recommendations);
    }
}
