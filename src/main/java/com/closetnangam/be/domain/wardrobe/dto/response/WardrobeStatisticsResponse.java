package com.closetnangam.be.domain.wardrobe.dto.response;

import java.util.List;

/**
 * 옷장 통계 응답.
 * {@code userStylePayloads}는 {@code USER_STYLES.wardrobe_weight} 반영용 계산값이며,
 * {@code preference_weight}, {@code feedback_weight}, {@code combined_weight}는 아직 미구현입니다.
 */
public record WardrobeStatisticsResponse(
        Long userId,
        Long wardrobeId,
        int totalOwnedCount,
        int totalWishlistCount,
        int totalWardrobeClothesCount,
        List<ItemTypeCount> itemTypes,
        List<UserStyleWardrobePayload> userStylePayloads
) {

    /** itemType(소분류)별 옷장 등록 옷 개수 */
    public record ItemTypeCount(
            String itemType,
            String itemTypeLabel,
            String category,
            int count
    ) {
    }

    /**
     * {@code USER_STYLES} 테이블의 {@code wardrobe_weight} 컬럼에 전달할 후보 값.
     * 스타일 태그 가중치: PRIMARY 0.7, SECONDARY 0.3 합산 후 0~100 정규화.
     */
    public record UserStyleWardrobePayload(
            Long styleId,
            String styleCode,
            String styleName,
            double weightedScore,
            int wardrobeWeight
    ) {
    }
}
