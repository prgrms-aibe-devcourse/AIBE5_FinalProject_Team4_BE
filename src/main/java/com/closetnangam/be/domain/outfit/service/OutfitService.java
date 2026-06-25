package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostRepository;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutfitService {

    static final String FEED_SAVE_SOURCE_PREFIX = "feed-save-source:";

    private final OutfitBookRepository outfitBookRepository;
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesRepository clothesRepository;
    private final FeedPostRepository feedPostRepository;
    private final UserRepository userRepository;
    private final OutfitStyleService outfitStyleService;  // OutfitStyleRepository 대신 공통 서비스 주입

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
            outfitStyleService.saveOutfitStyles(outfit, savedItems);  // 추가
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
            outfitStyleService.deleteOutfitStyles(outfitId);  // 공통 서비스로 변경
            if (request.getItems().isEmpty()) {
                currentItems = Collections.emptyList();
            } else {
                currentItems = saveOutfitItems(outfit, userId, request.getItems());
                outfitStyleService.saveOutfitStyles(outfit, currentItems);  // 공통 서비스로 변경
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

    /**
     * 공개 피드에 연결된 다른 사용자 코디를 내 코디북으로 복제합니다.
     * 본인 코디면 그대로 반환하고, 저장 가능한 아이템이 없으면 예외를 던집니다.
     */
    @Transactional
    public Outfit cloneOutfitToUserBook(Outfit source, Long userId) {
        if (source.getOutfitBook().getUser().getId().equals(userId)) {
            return source;
        }

        OutfitBook book = outfitBookRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
                    return outfitBookRepository.save(OutfitBook.create(user));
                });

        List<OutfitItem> sourceItems = outfitItemRepository.findAllByOutfit_OutfitId(source.getOutfitId());
        List<OutfitItemRequest> resolvableItems = resolveSavableItems(sourceItems, userId);

        if (resolvableItems.isEmpty() && !sourceItems.isEmpty()) {
            throw new IllegalArgumentException("저장할 수 있는 옷이 없습니다.");
        }

        Outfit clone = outfitRepository.save(Outfit.builder()
                .outfitBook(book)
                .title(StringUtils.hasText(source.getTitle()) ? source.getTitle() : "저장한 코디")
                .description(buildFeedSaveDescription(source))
                .thumbnailUrl(StringUtils.hasText(source.getThumbnailUrl()) ? source.getThumbnailUrl() : "")
                .situation(StringUtils.hasText(source.getSituation()) ? source.getSituation() : "DAILY")
                .season(StringUtils.hasText(source.getSeason()) ? source.getSeason() : "ALL")
                .favorite(false)
                .build());

        if (!resolvableItems.isEmpty()) {
            List<OutfitItem> savedItems = saveOutfitItems(clone, userId, resolvableItems, true);
            outfitStyleService.saveOutfitStyles(clone, savedItems);
        }
        return clone;
    }

    public boolean isFeedOutfitSavedByUser(Outfit source, Long userId) {
        if (source.getOutfitBook().getUser().getId().equals(userId)) {
            return true;
        }
        return findFeedSaveClone(source.getOutfitId(), userId).isPresent();
    }

    @Transactional
    public boolean toggleFeedOutfitSave(Outfit source, Long userId) {
        if (source.getOutfitBook().getUser().getId().equals(userId)) {
            return true;
        }

        Optional<Outfit> existingClone = findFeedSaveClone(source.getOutfitId(), userId);
        if (existingClone.isPresent()) {
            existingClone.get().softDelete();
            return false;
        }

        cloneOutfitToUserBook(source, userId);
        return true;
    }

    private Optional<Outfit> findFeedSaveClone(Long sourceOutfitId, Long userId) {
        return outfitRepository.findActiveFeedSaveClone(userId, feedSaveSourceMarker(sourceOutfitId));
    }

    private String buildFeedSaveDescription(Outfit source) {
        return feedSaveSourceMarker(source.getOutfitId());
    }

    private String feedSaveSourceMarker(Long sourceOutfitId) {
        return FEED_SAVE_SOURCE_PREFIX + sourceOutfitId;
    }

    private List<OutfitItemRequest> resolveSavableItems(List<OutfitItem> sourceItems, Long userId) {
        if (sourceItems.isEmpty()) {
            return List.of();
        }

        List<Long> clothesIds = sourceItems.stream()
                .map(item -> item.getClothes().getId())
                .distinct()
                .toList();

        Set<Long> ownedClothesIds = wardrobeClothesRepository
                .findAllByClothesIdsAndUserId(clothesIds, userId).stream()
                .map(item -> item.getClothes().getId())
                .collect(Collectors.toSet());

        List<OutfitItemRequest> resolved = new ArrayList<>();
        for (OutfitItem item : sourceItems) {
            Clothes clothes = item.getClothes();
            Long clothesId = clothes.getId();

            if (ownedClothesIds.contains(clothesId) || isSavableSharedClothes(clothes)) {
                resolved.add(new OutfitItemRequest(clothesId, item.getItemRole(), item.getLayerOrder()));
            }
        }
        return resolved;
    }

    private boolean isSavableSharedClothes(Clothes clothes) {
        ClothesInfoSource source = clothes.getClothesInfoSource();
        if (source == ClothesInfoSource.EXTERNAL_SHOPPING) {
            return true;
        }
        return source == ClothesInfoSource.PHOTO
                && feedPostRepository.existsByClothesIdInPublicFeed(clothes.getId());
    }

    private List<OutfitItem> saveOutfitItems(Outfit outfit, Long userId, List<OutfitItemRequest> itemRequests) {
        return saveOutfitItems(outfit, userId, itemRequests, false);
    }

    private List<OutfitItem> saveOutfitItems(
            Outfit outfit,
            Long userId,
            List<OutfitItemRequest> itemRequests,
            boolean allowPublicFeedSharedClothes
    ) {
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

                    // 2. WardrobeClothes에 없으면 외부 상품으로 저장된 공통 옷 정보만 허용
                    if (clothes == null) {
                        clothes = clothesRepository.findById(itemRequest.getClothesId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "옷을 찾을 수 없습니다. ID: " + itemRequest.getClothesId()));

                        if (!isAllowedSharedClothes(clothes, allowPublicFeedSharedClothes)) {
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

    private boolean isAllowedSharedClothes(Clothes clothes, boolean allowPublicFeedSharedClothes) {
        if (clothes.getClothesInfoSource() == ClothesInfoSource.EXTERNAL_SHOPPING) {
            return true;
        }
        return allowPublicFeedSharedClothes && isSavableSharedClothes(clothes);
    }

    public OutfitResponse getOutfit(Long bookId, Long outfitId, Long userId) {
        Outfit outfit = outfitRepository.findActiveByOutfitIdAndOutfitBook_Id(outfitId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코디입니다."));

        if (!outfit.getOutfitBook().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 코디만 조회할 수 있습니다.");
        }

        List<OutfitItem> outfitItems = outfitItemRepository.findAllByOutfitId(outfitId);
        Map<Long, WardrobeClothes> wardrobeClothesByClothesId = findWardrobeClothesByClothesId(userId, outfitItems);

        List<OutfitItemResponse> items = outfitItems.stream()
                .map(item -> OutfitItemResponse.from(item, wardrobeClothesByClothesId.get(item.getClothes().getId())))
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
                        (first, ignored) -> first
                ));
    }
}
