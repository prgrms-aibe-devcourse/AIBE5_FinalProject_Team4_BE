package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.recommendation.dto.request.RecommendationFeedbackRequest;
import com.closetnangam.be.domain.recommendation.entity.RecommendationFeedback;
import com.closetnangam.be.domain.recommendation.enums.FeedbackType;
import com.closetnangam.be.domain.recommendation.repository.RecommendationFeedbackRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationFeedbackService {

    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final UserRepository userRepository;
    private final ClothesRepository clothesRepository;
    private final UserStyleRepository userStyleRepository;

    @Transactional
    public void submitFeedback(Long userId, RecommendationFeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        Clothes clothes = clothesRepository.findById(request.clothesId())
                .orElseThrow(() -> new EntityNotFoundException("옷을 찾을 수 없습니다."));

        // 중복 피드백 방지
        if (isAlreadyFeedbacked(userId, clothes.getId(), request.feedbackType())) {
            return;
        }

        // 기존 피드백 있으면 업데이트, 없으면 새로 생성
        RecommendationFeedback feedback = recommendationFeedbackRepository
                .findByUserIdAndClothesId(userId, clothes.getId())
                .orElseGet(() -> RecommendationFeedback.builder()
                        .user(user)
                        .clothes(clothes)
                        .build());

        updateFeedbackState(feedback, request.feedbackType());
        recommendationFeedbackRepository.save(feedback);

        // 스타일 가중치 업데이트
        int weightDelta = calculateWeightDelta(request.feedbackType());
        if (weightDelta != 0) {
            updateUserStyleWeights(user, clothes, weightDelta);
        }
    }

    private boolean isAlreadyFeedbacked(Long userId, Long clothesId, FeedbackType type) {
        return switch (type) {
            case SAVED -> recommendationFeedbackRepository.existsByUserIdAndClothesIdAndSavedIsTrue(userId, clothesId);
            case DISLIKE -> recommendationFeedbackRepository.existsByUserIdAndClothesIdAndDislikedIsTrue(userId, clothesId);
            case EXCLUDE -> recommendationFeedbackRepository.existsByUserIdAndClothesIdAndExcludedIsTrue(userId, clothesId);
        };
    }

    private void updateFeedbackState(RecommendationFeedback feedback, FeedbackType type) {
        switch (type) {
            case SAVED -> feedback.updateSaved(true);
            case DISLIKE -> feedback.updateDisliked(true);
            case EXCLUDE -> feedback.updateExcluded(true);
        }
    }

    private int calculateWeightDelta(FeedbackType type) {
        return switch (type) {
            case SAVED -> 0;
            case DISLIKE, EXCLUDE -> -1;
        };
    }

    private void updateUserStyleWeights(User user, Clothes clothes, int delta) {
        List<ClothesStyleTag> styleTags = clothes.getSortedStyleTags();
        for (ClothesStyleTag tag : styleTags) {
            UserStyle userStyle = userStyleRepository.findByUserIdAndStyleId(user.getId(), tag.getStyle().getId())
                    .orElseGet(() -> userStyleRepository.save(UserStyle.builder()
                            .user(user)
                            .style(tag.getStyle())
                            .build()));

            int amount = (tag.getStyleRole() == StyleRole.PRIMARY) ? 7 : 3;
            int newWeight = userStyle.getFeedbackWeight() + (amount * delta);
            userStyle.updateFeedbackWeight(newWeight);
        }
    }
}
