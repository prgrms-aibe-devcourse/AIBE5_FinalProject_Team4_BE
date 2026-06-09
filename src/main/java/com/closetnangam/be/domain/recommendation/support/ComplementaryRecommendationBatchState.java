package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;

import java.time.Instant;
import java.util.List;

/**
 * Batch 제출 시 저장해 두었다가 import 단계에서 상품 메타데이터를 복원한다.
 */
public record ComplementaryRecommendationBatchState(
        String jobName,
        String model,
        Instant submittedAt,
        List<NaverShoppingProductResponse> products
) {
}
