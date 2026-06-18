package com.closetnangam.be.domain.user.entity;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_styles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_styles_user_style",
                columnNames = {"user_id", "style_id"}
        )
)
public class UserStyle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "preference_weight", nullable = false)
    private Integer preferenceWeight = 0;

    @Column(name = "wardrobe_weight", nullable = false)
    private Integer wardrobeWeight = 0;

    @Column(name = "feedback_weight", nullable = false)
    private Integer feedbackWeight = 0;

    @Column(name = "combined_weight", nullable = false)
    private Integer combinedWeight = 0;

    @Builder
    public UserStyle(User user, Style style) {
        this.user = user;
        this.style = style;
        this.preferenceWeight = 0;
        this.wardrobeWeight = 0;
        this.feedbackWeight = 0;
        this.combinedWeight = 0;
    }

    public void updatePreferenceWeight(int weight) {
        this.preferenceWeight = weight;
        calculateCombinedWeight();
    }

    public void updateWardrobeWeight(int weight) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight();
    }

    public void syncWardrobeWeight(int weight) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight();
    }

    public void updateFeedbackWeight(int weight) {
        this.feedbackWeight = weight;
        calculateCombinedWeight();
    }

    private void calculateCombinedWeight() {
        // 옷장 통계가 있는 경우 (wardrobeWeight > 0)
        // 보유 옷 비율 70% + 온보딩 가중치 30% 배분 전략 적용
        // 온보딩 가중치(preferenceWeight)는 1.0~5.0 범위라고 하셨으나 DB에는 Integer(예: 100~500 또는 1~5)로 저장될 수 있음.
        // 여기서는 기존 combinedWeight가 단순히 합산이었으므로, 비율로 재계산.
        if (this.wardrobeWeight > 0) {
            // 기존 preferenceWeight가 1~5 범위라면 100을 곱해 단위를 맞추거나, 
            // 여기선 단순히 가중치 비율만 조정.
            // (preferenceWeight * 0.3) + (wardrobeWeight * 0.7) + (feedbackWeight)
            double combined = (this.preferenceWeight * 0.3) + (this.wardrobeWeight * 0.7) + this.feedbackWeight;
            this.combinedWeight = (int) Math.round(combined);
        } else {
            // 신규 사용자(옷장 비어있음): 온보딩 가중치 100% 반영
            this.combinedWeight = this.preferenceWeight + this.feedbackWeight;
        }
    }
}