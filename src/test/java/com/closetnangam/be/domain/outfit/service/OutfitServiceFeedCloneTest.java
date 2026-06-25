package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostRepository;
import com.closetnangam.be.domain.outfit.dto.request.OutfitItemRequest;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutfitServiceFeedCloneTest {

    @Mock private OutfitBookRepository outfitBookRepository;
    @Mock private OutfitRepository outfitRepository;
    @Mock private OutfitItemRepository outfitItemRepository;
    @Mock private WardrobeClothesRepository wardrobeClothesRepository;
    @Mock private ClothesRepository clothesRepository;
    @Mock private FeedPostRepository feedPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private OutfitStyleService outfitStyleService;

    @InjectMocks
    private OutfitService outfitService;

    @Test
    @DisplayName("공개 피드 PHOTO 옷은 피드 코디 복제 저장 대상에 포함된다")
    void resolveSavableItems_includesPublicFeedPhoto() throws Exception {
        Clothes photoClothes = mock(Clothes.class);
        given(photoClothes.getId()).willReturn(99L);
        given(photoClothes.getClothesInfoSource()).willReturn(ClothesInfoSource.PHOTO);

        OutfitItem item = mock(OutfitItem.class);
        given(item.getClothes()).willReturn(photoClothes);
        given(item.getItemRole()).willReturn("TOP");
        given(item.getLayerOrder()).willReturn(1);

        given(wardrobeClothesRepository.findAllByClothesIdsAndUserId(List.of(99L), 2L)).willReturn(List.of());
        given(feedPostRepository.existsByClothesIdInPublicFeed(99L)).willReturn(true);

        Method method = OutfitService.class.getDeclaredMethod("resolveSavableItems", List.class, Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<OutfitItemRequest> resolved = (List<OutfitItemRequest>) method.invoke(outfitService, List.of(item), 2L);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().getClothesId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("피드 코디 저장 시 OUTFITS.description 마커로 원본 코디를 추적한다")
    void toggleFeedOutfitSave_createsCloneWithDescriptionMarker() {
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        OutfitBook ownerBook = mock(OutfitBook.class);
        given(ownerBook.getUser()).willReturn(owner);

        Outfit source = Outfit.builder()
                .outfitBook(ownerBook)
                .title("피드 코디")
                .description("원본")
                .thumbnailUrl("")
                .situation("DAILY")
                .season("ALL")
                .favorite(false)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(source, "outfitId", 50L);

        OutfitBook saverBook = mock(OutfitBook.class);
        given(outfitRepository.findActiveFeedSaveClone(2L, "feed-save-source:50:")).willReturn(Optional.empty());
        given(outfitBookRepository.findByUser_Id(2L)).willReturn(Optional.of(saverBook));
        given(outfitItemRepository.findAllByOutfit_OutfitId(50L)).willReturn(List.of());
        given(outfitRepository.save(any(Outfit.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(outfitService.toggleFeedOutfitSave(source, 2L)).isTrue();

        ArgumentCaptor<Outfit> outfitCaptor = ArgumentCaptor.forClass(Outfit.class);
        verify(outfitRepository).save(outfitCaptor.capture());
        assertThat(outfitCaptor.getValue().getDescription()).isEqualTo("feed-save-source:50:");
    }

    @Test
    @DisplayName("feed-save-source 마커는 원본 코디 ID별로 prefix 충돌 없이 구분된다")
    void feedSaveSourceMarker_distinguishesNestedNumericIds() throws Exception {
        Method method = OutfitService.class.getDeclaredMethod("feedSaveSourceMarker", Long.class);
        method.setAccessible(true);

        String markerFor5 = (String) method.invoke(outfitService, 5L);
        String markerFor50 = (String) method.invoke(outfitService, 50L);
        String markerFor500 = (String) method.invoke(outfitService, 500L);

        assertThat(markerFor5).isEqualTo("feed-save-source:5:");
        assertThat(markerFor50).isEqualTo("feed-save-source:50:");
        assertThat(markerFor500).isEqualTo("feed-save-source:500:");
        assertThat(markerFor50).doesNotStartWith(markerFor5);
        assertThat(markerFor500).doesNotStartWith(markerFor5);
        assertThat(markerFor500).doesNotStartWith(markerFor50);
    }

    @Test
    @DisplayName("저장 여부 확인 시 원본 코디 5번은 50번 저장본과 prefix 충돌하지 않는다")
    void isFeedOutfitSavedByUser_queriesExactMarkerForSourceOutfitId() {
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        OutfitBook ownerBook = mock(OutfitBook.class);
        given(ownerBook.getUser()).willReturn(owner);

        Outfit source = Outfit.builder()
                .outfitBook(ownerBook)
                .title("피드 코디")
                .description("원본")
                .thumbnailUrl("")
                .situation("DAILY")
                .season("ALL")
                .favorite(false)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(source, "outfitId", 5L);

        given(outfitRepository.findActiveFeedSaveClone(2L, "feed-save-source:5:")).willReturn(Optional.empty());

        assertThat(outfitService.isFeedOutfitSavedByUser(source, 2L)).isFalse();

        verify(outfitRepository).findActiveFeedSaveClone(eq(2L), eq("feed-save-source:5:"));
        verify(outfitRepository, never()).findActiveFeedSaveClone(eq(2L), eq("feed-save-source:50:"));
    }

    @Test
    @DisplayName("피드 코디 저장 취소 시 description 마커로 찾은 복제본을 소프트 삭제한다")
    void toggleFeedOutfitSave_softDeletesExistingClone() {
        User owner = mock(User.class);
        given(owner.getId()).willReturn(1L);

        OutfitBook ownerBook = mock(OutfitBook.class);
        given(ownerBook.getUser()).willReturn(owner);

        Outfit source = Outfit.builder()
                .outfitBook(ownerBook)
                .title("피드 코디")
                .description("원본")
                .thumbnailUrl("")
                .situation("DAILY")
                .season("ALL")
                .favorite(false)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(source, "outfitId", 50L);

        Outfit existingClone = mock(Outfit.class);
        given(outfitRepository.findActiveFeedSaveClone(2L, "feed-save-source:50:"))
                .willReturn(Optional.of(existingClone));

        assertThat(outfitService.toggleFeedOutfitSave(source, 2L)).isFalse();
        verify(existingClone).softDelete();
    }
}
