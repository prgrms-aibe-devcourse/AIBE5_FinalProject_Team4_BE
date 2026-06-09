package com.closetnangam.be.domain.recommendation.entity;

import com.closetnangam.be.domain.clothes.entity.Clothes;

import com.closetnangam.be.domain.recommendation.enums.FeedbackType;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recommendation_feedbacks")
public class RecommendationFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id")
    private Clothes clothes;

    @Column(nullable = false)
    private boolean disliked = false;

    private LocalDateTime dislikedAt;

    @Column(nullable = false)
    private boolean excluded = false;

    private LocalDateTime excludedAt;

    @Column(nullable = false)
    private boolean saved = false;

    private LocalDateTime savedAt;

    @Builder
    public RecommendationFeedback(User user, Clothes clothes) {
        this.user = user;
        this.clothes = clothes;
    }

    public void updateDisliked(boolean disliked) {
        this.disliked = disliked;
        this.dislikedAt = disliked ? LocalDateTime.now() : null;
    }

    public void updateExcluded(boolean excluded) {
        this.excluded = excluded;
        this.excludedAt = excluded ? LocalDateTime.now() : null;
    }

    public void updateSaved(boolean saved) {
        this.saved = saved;
        this.savedAt = saved ? LocalDateTime.now() : null;
    }
}