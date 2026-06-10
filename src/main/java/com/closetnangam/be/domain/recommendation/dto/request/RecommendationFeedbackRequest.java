package com.closetnangam.be.domain.recommendation.dto.request;

import com.closetnangam.be.domain.recommendation.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;

public record RecommendationFeedbackRequest(
        @NotNull(message = "옷 ID는 필수입니다.")
        Long clothesId,

        @NotNull(message = "피드백 타입은 필수입니다.")
        FeedbackType feedbackType
) {
}
