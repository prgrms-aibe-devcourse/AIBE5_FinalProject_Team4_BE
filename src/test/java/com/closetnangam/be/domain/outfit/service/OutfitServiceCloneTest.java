package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.outfit.dto.request.OutfitCreateRequest;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutfitServiceCloneTest {

    @Mock private OutfitBookRepository outfitBookRepository;
    @Mock private OutfitRepository outfitRepository;
    @Mock private OutfitItemRepository outfitItemRepository;
    @Mock private WardrobeClothesRepository wardrobeClothesRepository;
    @Mock private ClothesRepository clothesRepository;
    @Mock private UserRepository userRepository;
    @Mock private OutfitStyleService outfitStyleService;

    @InjectMocks
    private OutfitService outfitService;

    @Test
    @DisplayName("코디 생성 시 구성 옷 스타일이 OUTFIT_STYLES에 저장된다")
    void createOutfit_savesOutfitStyles() {
        Long bookId = 1L;
        Long userId = 1L;

        OutfitBook book = mock(OutfitBook.class);
        given(book.getId()).willReturn(bookId);
        given(outfitBookRepository.findByIdAndUserId(bookId, userId)).willReturn(Optional.of(book));

        OutfitCreateRequest request = mock(OutfitCreateRequest.class);
        OutfitItemRequest itemRequest = new OutfitItemRequest(10L, "TOP", 1);
        given(request.getItems()).willReturn(List.of(itemRequest));

        Outfit outfit = Outfit.builder()
                .outfitBook(book)
                .title("코디")
                .description("설명")
                .thumbnailUrl("")
                .situation("DAILY")
                .season("SPRING")
                .favorite(false)
                .build();
        given(request.toEntity(book)).willReturn(outfit);
        given(outfitRepository.save(any(Outfit.class))).willReturn(outfit);

        Clothes clothes = mock(Clothes.class);
        given(clothes.getId()).willReturn(10L);
        WardrobeClothes wardrobeClothes = mock(WardrobeClothes.class);
        given(wardrobeClothes.getClothes()).willReturn(clothes);
        given(wardrobeClothesRepository.findAllByClothesIdsAndUserId(List.of(10L), userId))
                .willReturn(List.of(wardrobeClothes));
        given(outfitItemRepository.saveAll(anyList())).willReturn(List.of());

        outfitService.createOutfit(bookId, userId, request);

        ArgumentCaptor<List<OutfitItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(outfitItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().getFirst().getClothes()).isSameAs(clothes);
        verify(outfitStyleService).saveOutfitStyles(eq(outfit), eq(List.of()));
    }
}
