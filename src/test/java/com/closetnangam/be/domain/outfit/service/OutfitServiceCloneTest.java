package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
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
    @DisplayName("피드 저장으로 코디 복제 시 OUTFIT_STYLES가 생성된다")
    void cloneOutfitToUserBook_savesOutfitStyles() {
        Long ownerId = 1L;
        Long saverId = 2L;

        // 원본 코디 소유자
        User owner = mock(User.class);
        given(owner.getId()).willReturn(ownerId);

        OutfitBook ownerBook = mock(OutfitBook.class);
        given(ownerBook.getUser()).willReturn(owner);

        Clothes clothes = mock(Clothes.class);
        given(clothes.getId()).willReturn(10L);

        OutfitItem sourceItem = mock(OutfitItem.class);
        given(sourceItem.getClothes()).willReturn(clothes);
        given(sourceItem.getItemRole()).willReturn("TOP");
        given(sourceItem.getLayerOrder()).willReturn(1);

        Outfit source = mock(Outfit.class);
        given(source.getOutfitBook()).willReturn(ownerBook);
        given(source.getOutfitId()).willReturn(100L);
        given(source.getTitle()).willReturn("피드 코디");
        given(source.getDescription()).willReturn("설명");
        given(source.getThumbnailUrl()).willReturn("http://example.com/img.jpg");
        given(source.getSituation()).willReturn("DAILY");
        given(source.getSeason()).willReturn("ALL");

        // 저장자 코디북
        OutfitBook saverBook = mock(OutfitBook.class);
        given(outfitBookRepository.findByUser_Id(saverId)).willReturn(Optional.of(saverBook));

        given(outfitItemRepository.findAllByOutfit_OutfitId(100L)).willReturn(List.of(sourceItem));
        given(wardrobeClothesRepository.findAllByClothesIdsAndUserId(List.of(10L), saverId))
                .willReturn(List.of());
        given(clothesRepository.findById(10L)).willReturn(Optional.of(clothes));

        Outfit clone = mock(Outfit.class);
        given(outfitRepository.save(any(Outfit.class))).willReturn(clone);

        OutfitItem savedItem = mock(OutfitItem.class);
        given(outfitItemRepository.saveAll(anyList())).willReturn(List.of(savedItem));

        // 실행
        outfitService.cloneOutfitToUserBook(source, saverId);

        // 검증: saveOutfitStyles(clone, savedItems)가 호출되었는지
        ArgumentCaptor<List<OutfitItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(outfitStyleService).saveOutfitStyles(eq(clone), itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).containsExactly(savedItem);
    }

    @Test
    @DisplayName("본인 코디 저장 시 복제 없이 원본을 반환하고 스타일 저장을 호출하지 않는다")
    void cloneOutfitToUserBook_ownOutfit_returnsSourceWithoutClone() {
        Long userId = 1L;

        User owner = mock(User.class);
        given(owner.getId()).willReturn(userId);

        OutfitBook book = mock(OutfitBook.class);
        given(book.getUser()).willReturn(owner);

        Outfit source = mock(Outfit.class);
        given(source.getOutfitBook()).willReturn(book);

        Outfit result = outfitService.cloneOutfitToUserBook(source, userId);

        assertThat(result).isSameAs(source);
        org.mockito.Mockito.verifyNoInteractions(outfitStyleService);
    }
}
