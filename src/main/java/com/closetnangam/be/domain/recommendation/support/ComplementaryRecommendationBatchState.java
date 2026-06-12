package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Batch 제출 시 저장해 두었다가 import 단계에서 상품 메타데이터를 복원한다.
 *
 * @param collectionMode {@code random} | {@code gap-fill}. null이면 random.
 * @param productBuckets gap-fill 수집 시 productId → bucket id 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComplementaryRecommendationBatchState(
        String jobName,
        String model,
        Instant submittedAt,
        List<NaverShoppingProductResponse> products,
        String collectionMode,
        Map<String, String> productBuckets
) {

    public static final String MODE_RANDOM = "random";
    public static final String MODE_GAP_FILL = "gap-fill";

    public boolean isGapFill() {
        return MODE_GAP_FILL.equalsIgnoreCase(collectionMode);
    }

    public Map<String, String> productBucketsOrEmpty() {
        return productBuckets == null ? Map.of() : productBuckets;
    }
}
