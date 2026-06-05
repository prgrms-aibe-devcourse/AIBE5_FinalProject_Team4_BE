package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StyleProductRecommenderTest {

    @Mock
    private ClothesRepository clothesRepository;
    @Mock
    private WardrobeClothesRepository wardrobeClothesRepository;
    @Mock
    private WardrobeRepository wardrobeRepository;
    @Mock
    private UserStyleRepository userStyleRepository;

    @InjectMocks
    private StyleProductRecommender styleProductRecommender;

    @Test
    @DisplayName("사용자의 스타일 가중치(combined_weight)가 높은 스타일 상품이 더 높은 점수를 받는다")
    void recommendByStyleUsesUserStyleWeights() {
        // Given
        Long userId = 10L;
        Long wardrobeId = 1L;
        User user = User.builder().nickname("tester").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Wardrobe wardrobe = Wardrobe.create(user);
        ReflectionTestUtils.setField(wardrobe, "id", wardrobeId);

        // 사용자 스타일 가중치 설정: MINIMAL(100), CASUAL(0)
        UserStyle minimalStyleWeight = UserStyle.builder().user(user).styleCode(StyleCode.MINIMAL).build();
        minimalStyleWeight.updatePreferenceWeight(100);

        given(wardrobeRepository.findById(wardrobeId)).willReturn(Optional.of(wardrobe));
        given(userStyleRepository.findAllByUserId(userId)).willReturn(List.of(minimalStyleWeight));
        given(wardrobeClothesRepository.findAllByWardrobeId(wardrobeId)).willReturn(Collections.emptyList());

        // 후보 1: MINIMAL 스타일
        Clothes candidateMinimal = Clothes.builder()
                .name("Minimal Item")
                .itemType("SHIRT")
                .infoSource(ClothesInfoSource.EXTERNAL_SHOPPING)
                .build();
        ReflectionTestUtils.setField(candidateMinimal, "id", 101L);
        candidateMinimal.addStyleTag(ClothesStyleTag.create(candidateMinimal, Style.from(StyleCode.MINIMAL), StyleRole.PRIMARY, (byte) 0));

        // 후보 2: CASUAL 스타일
        Clothes candidateCasual = Clothes.builder()
                .name("Casual Item")
                .itemType("SHIRT")
                .infoSource(ClothesInfoSource.EXTERNAL_SHOPPING)
                .build();
        ReflectionTestUtils.setField(candidateCasual, "id", 102L);
        candidateCasual.addStyleTag(ClothesStyleTag.create(candidateCasual, Style.from(StyleCode.CASUAL), StyleRole.PRIMARY, (byte) 0));

        given(clothesRepository.findAllForRecommendation(any(PageRequest.class)))
                .willReturn(List.of(candidateMinimal, candidateCasual));

        // When
        List<RecommendResponse> results = styleProductRecommender.recommendByStyle(userId, wardrobeId, 20.0);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).title()).isEqualTo("Minimal Item");
        
        double scoreMinimal = Double.parseDouble(results.get(0).score());
        double scoreCasual = Double.parseDouble(results.get(1).score());
        assertThat(scoreMinimal).isGreaterThan(scoreCasual);
    }

    @Test
    @DisplayName("후보군 로드 시 설정된 CANDIDATE_LIMIT 만큼 요청한다")
    void recommendByStyleRequestsCandidatesWithLimit() {
        // Given
        Long userId = 10L;
        Long wardrobeId = 1L;
        User user = User.builder().nickname("tester").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Wardrobe wardrobe = Wardrobe.create(user);
        ReflectionTestUtils.setField(wardrobe, "id", wardrobeId);

        given(wardrobeRepository.findById(wardrobeId)).willReturn(Optional.of(wardrobe));
        given(userStyleRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());
        given(wardrobeClothesRepository.findAllByWardrobeId(wardrobeId)).willReturn(Collections.emptyList());

        // When
        styleProductRecommender.recommendByStyle(userId, wardrobeId, 20.0);

        // Then
        org.mockito.ArgumentCaptor<PageRequest> pageRequestCaptor = org.mockito.ArgumentCaptor.forClass(PageRequest.class);
        org.mockito.Mockito.verify(clothesRepository).findAllForRecommendation(pageRequestCaptor.capture());
        
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(500);
    }
}
