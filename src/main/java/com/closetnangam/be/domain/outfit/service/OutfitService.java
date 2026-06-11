package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.outfit.dto.request.OutfitCreateRequest;
import com.closetnangam.be.domain.outfit.dto.request.OutfitItemRequest;
import com.closetnangam.be.domain.outfit.dto.request.OutfitUpdateRequest;
import com.closetnangam.be.domain.outfit.dto.response.OutfitBookResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitItemResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutfitService {

    private final OutfitBookRepository outfitBookRepository;
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesRepository clothesRepository;
    private final UserRepository userRepository;

    @Transactional
    public OutfitBookResponse createBook(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (outfitBookRepository.findByUser_Id(userId).isPresent()) {
            throw new IllegalStateException("이미 코디북이 존재합니다.");
        }

        OutfitBook outfitBook = outfitBookRepository.save(OutfitBook.create(user));
        return OutfitBookResponse.from(outfitBook, userId, List.of());
    }

    @Transactional
    public OutfitResponse createOutfit(Long bookId, Long userId, OutfitCreateRequest request) {
        OutfitBook outfitBook = outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));

        Outfit outfit = outfitRepository.save(request.toEntity(outfitBook));

        List<OutfitItem> savedItems = Collections.emptyList();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            savedItems = saveOutfitItems(outfit, userId, request.getItems());
        }

        return toOutfitResponse(outfit, userId, savedItems);
    }

    @Transactional
    public OutfitResponse updateOutfit(Long bookId, Long outfitId, Long userId, OutfitUpdateRequest request) {
        outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));

        Outfit outfit = outfitRepository.findActiveByOutfitIdAndOutfitBook_Id(outfitId, bookId)
                .orElseThrow(() -> new EntityNotFoundException("코디를 찾을 수 없습니다."));

        outfit.update(
                request.getTitle(),
                request.getDescription(),
                request.getThumbnailUrl(),
                request.getSituation(),
                request.getSeason(),
                Boolean.TRUE.equals(request.getFavorite())
        );

        List<OutfitItem> currentItems;
        if (request.getItems() != null) {
            outfitItemRepository.deleteAllByOutfit_OutfitId(outfitId);
            if (request.getItems().isEmpty()) {
                currentItems = Collections.emptyList();
            } else {
                currentItems = saveOutfitItems(outfit, userId, request.getItems());
            }
        } else {
            // items가 null인 경우 기존 구성 유지
            currentItems = outfitItemRepository.findAllByOutfit_OutfitId(outfitId);
        }

        return toOutfitResponse(outfit, userId, currentItems);
    }

    private OutfitResponse toOutfitResponse(Outfit outfit, Long userId, List<OutfitItem> items) {
        if (items.isEmpty()) {
            return OutfitResponse.from(outfit);
        }

        Map<Long, WardrobeClothes> wardrobeClothesByClothesId = findWardrobeClothesByClothesId(userId, items);
        List<OutfitItemResponse> itemResponses = items.stream()
                .map(item -> OutfitItemResponse.from(item, wardrobeClothesByClothesId.get(item.getClothes().getId())))
                .toList();

        return OutfitResponse.from(outfit, itemResponses);
    }

    @Transactional
    public void deleteOutfit(Long bookId, Long outfitId, Long userId) {
        outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));

        Outfit outfit = outfitRepository.findActiveByOutfitIdAndOutfitBook_Id(outfitId, bookId)
                .orElseThrow(() -> new EntityNotFoundException("코디를 찾을 수 없습니다."));

        outfit.softDelete();
    }

    private List<OutfitItem> saveOutfitItems(Outfit outfit, Long userId, List<OutfitItemRequest> itemRequests) {
        List<Long> clothesIds = itemRequests.stream()
                .map(OutfitItemRequest::getClothesId)
                .toList();

        // 1. 유저 소유 옷장에서 먼저 조회
        Map<Long, Clothes> clothesMap = wardrobeClothesRepository
                .findAllByClothesIdsAndUserId(clothesIds, userId).stream()
                .map(WardrobeClothes::getClothes)
                .collect(Collectors.toMap(Clothes::getId, c -> c, (first, second) -> first));

        List<OutfitItem> items = itemRequests.stream()
                .map(itemRequest -> {
                    Clothes clothes = clothesMap.get(itemRequest.getClothesId());

                    // 2. WardrobeClothes에 없으면 EXTERNAL_SHOPPING만 폴백 허용
                    // PHOTO / PURCHASE_HISTORY는 반드시 본인 옷장 소유여야 함
                    if (clothes == null) {
                        clothes = clothesRepository.findById(itemRequest.getClothesId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "옷을 찾을 수 없습니다. ID: " + itemRequest.getClothesId()));

                        if (clothes.getClothesInfoSource() != ClothesInfoSource.EXTERNAL_SHOPPING) {
                            throw new EntityNotFoundException(
                                    "사용자 옷장에서 옷을 찾을 수 없습니다. ID: " + itemRequest.getClothesId());
                        }
                    }

                    return OutfitItem.builder()
                            .outfit(outfit)
                            .clothes(clothes)
                            .itemRole(itemRequest.getItemRole())
                            .layerOrder(itemRequest.getLayerOrder())
                            .build();
                })
                .toList();
        return outfitItemRepository.saveAll(items);
    }
    public OutfitResponse getOutfit(Long bookId, Long outfitId, Long userId) {
        Outfit outfit = outfitRepository.findActiveByOutfitIdAndOutfitBook_Id(outfitId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코디입니다."));

        if (!outfit.getOutfitBook().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 코디만 조회할 수 있습니다.");
        }

        List<OutfitItemResponse> items = outfitItemRepository.findAllByOutfitId(outfitId)
                .stream()
                .map(item -> {
                    WardrobeClothes wardrobeClothes = wardrobeClothesRepository
                            .findByWardrobe_User_IdAndClothes_Id(userId, item.getClothes().getId())
                            .orElse(null);
                    return OutfitItemResponse.from(item, wardrobeClothes);
                })
                .toList();

        return OutfitResponse.from(outfit, items);
    }

    public OutfitBookResponse getBookByUserId(Long userId) {
        OutfitBook outfitBook = outfitBookRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));
        return toBookResponse(outfitBook, userId);
    }

    public OutfitBookResponse getBookById(Long bookId, Long userId) {
        OutfitBook outfitBook = outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));
        return toBookResponse(outfitBook, userId);
    }

    private OutfitBookResponse toBookResponse(OutfitBook outfitBook, Long userId) {
        List<Outfit> outfits = outfitRepository.findAllByOutfitBookId(outfitBook.getId());
        if (outfits.isEmpty()) {
            return OutfitBookResponse.from(outfitBook, userId, outfits);
        }

        /*
         * AI MD가 저장한 코디는 OUTFIT_ITEMS에 실제 구성 옷을 남긴다.
         * 코디북 재조회에서도 FE가 저장된 코디를 복원할 수 있도록 outfitId별 구성 아이템을 함께 내려준다.
         */
        List<OutfitItem> outfitItems = outfitItemRepository.findAllByOutfitBookId(outfitBook.getId());
        Map<Long, WardrobeClothes> wardrobeClothesByClothesId = findWardrobeClothesByClothesId(userId, outfitItems);
        Map<Long, List<OutfitItemResponse>> itemResponsesByOutfitId = outfitItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getOutfit().getOutfitId(),
                        Collectors.mapping(
                                item -> OutfitItemResponse.from(item, wardrobeClothesByClothesId.get(item.getClothes().getId())),
                                Collectors.toList()
                        )
                ));

        List<OutfitResponse> outfitResponses = outfits.stream()
                .map(outfit -> OutfitResponse.from(
                        outfit,
                        itemResponsesByOutfitId.getOrDefault(outfit.getOutfitId(), List.of())
                ))
                .toList();
        return OutfitBookResponse.from(outfitBook, userId, outfits, outfitResponses);
    }

    private Map<Long, WardrobeClothes> findWardrobeClothesByClothesId(Long userId, List<OutfitItem> outfitItems) {
        List<Long> clothesIds = outfitItems.stream()
                .map(item -> item.getClothes().getId())
                .distinct()
                .toList();
        if (clothesIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return wardrobeClothesRepository.findAllByClothesIdsAndUserId(clothesIds, userId).stream()
                .collect(Collectors.toMap(
                        item -> item.getClothes().getId(),
                        item -> item,
                        /*
                         * 동일 Clothes가 사용자 옷장에 중복 연결된 비정상 데이터가 있어도 코디북 조회는 실패하지 않게 한다.
                         * 먼저 조회된 활성 WardrobeClothes를 대표 사용자 소유 정보로 사용한다.
                         */
                        (first, ignored) -> first
                ));
    }
}
