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

    public void updatePreferenceWeight(int weight, boolean hasWardrobeData) {
        this.preferenceWeight = weight;
        calculateCombinedWeight(hasWardrobeData);
    }

    public void updateWardrobeWeight(int weight, boolean hasWardrobeData) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight(hasWardrobeData);
    }

    public void syncWardrobeWeight(int weight, boolean hasWardrobeData) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight(hasWardrobeData);
    }

    public void updateFeedbackWeight(int weight, boolean hasWardrobeData) {
        this.feedbackWeight = weight;
        calculateCombinedWeight(hasWardrobeData);
    }

    private void calculateCombinedWeight(boolean hasWardrobeData) {
        if (hasWardrobeData) {
            // 옷장 스타일 가중치 70% + 온보딩 가중치 30% 배분 전략 적용
            double combined = (this.preferenceWeight * 0.3) + (this.wardrobeWeight * 0.7);
            int base = (int) Math.round(combined);
            // preferenceWeight가 있으면 최소 1 보장
            if (this.preferenceWeight > 0 && base == 0) base = 1;
            this.combinedWeight = base + this.feedbackWeight;
        } else {
            this.combinedWeight = this.preferenceWeight + this.feedbackWeight;
        }
    }
}
