package com.closetnangam.be.domain.recommendation.repository;

import com.closetnangam.be.domain.recommendation.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

    boolean existsByUserIdAndClothesIdAndSavedIsTrue(Long userId, Long clothesId);
    boolean existsByUserIdAndClothesIdAndDislikedIsTrue(Long userId, Long clothesId);
    boolean existsByUserIdAndClothesIdAndExcludedIsTrue(Long userId, Long clothesId);

    Optional<RecommendationFeedback> findByUserIdAndClothesId(Long userId, Long clothesId);

    List<RecommendationFeedback> findAllByUserId(Long userId);

}
