package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import com.closetnangam.be.domain.clothes.dto.request.ClothesConvertToOwnedRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.request.WishlistClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesService {

    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesTagHelper clothesTagHelper;
    private final WardrobeService wardrobeService;

    public List<ClothesResponse> getOwnedClothes(Long userId) {
        return wardrobeClothesRepository.findAllByUserIdAndOwnershipStatus(userId, OwnershipStatus.OWNED).stream()
                .map(entry -> ClothesResponse.from(entry.getClothes(), entry))
                .toList();
    }

    public List<ClothesResponse> getFavoriteOwnedClothes(Long userId) {
        return wardrobeClothesRepository.findFavoritesByUserIdAndOwnershipStatus(userId, OwnershipStatus.OWNED).stream()
                .map(entry -> ClothesResponse.from(entry.getClothes(), entry))
                .toList();
    }

    public List<ClothesResponse> getWishlistClothes(Long userId) {
        return wardrobeClothesRepository.findAllByUserIdAndOwnershipStatus(userId, OwnershipStatus.WISHLIST).stream()
                .map(entry -> ClothesResponse.from(entry.getClothes(), entry))
                .toList();
    }

    public List<ClothesResponse> getFavoriteWishlistClothes(Long userId) {
        return wardrobeClothesRepository.findFavoritesByUserIdAndOwnershipStatus(userId, OwnershipStatus.WISHLIST).stream()
                .map(entry -> ClothesResponse.from(entry.getClothes(), entry))
                .toList();
    }

    public ClothesResponse getClothes(Long userId, Long clothesId) {
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdAndUserId(clothesId, userId)
                .orElseThrow(() -> new NoSuchElementException("옷을 찾을 수 없습니다."));
        return ClothesResponse.from(wardrobeClothes.getClothes(), wardrobeClothes);
    }

    @Transactional
    public ClothesResponse createOwnedClothes(Long userId, ClothesCreateRequest request) {
        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        Clothes clothes = buildClothes(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                ClothesInfoSource.PURCHASE_HISTORY,
                Clothes.EXTERNAL_NONE,
                Clothes.EXTERNAL_NONE,
                Clothes.EXTERNAL_NONE,
                request.isVerified(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );
        Clothes savedClothes = clothesRepository.save(clothes);

        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.save(WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(savedClothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size(request.size())
                .season(request.season())
                .favorite(false)
                .userImageUrl(request.imageUrl())
                .build());

        return ClothesResponse.from(savedClothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse createWishlistClothes(Long userId, WishlistClothesCreateRequest request) {
        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );
        clothesTagHelper.validateExternalSource(request.externalSource());

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        Clothes clothes = buildClothes(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                ClothesInfoSource.EXTERNAL_SHOPPING,
                request.externalSource(),
                request.externalProductId(),
                request.externalProductUrl(),
                false,
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );
        Clothes savedClothes = clothesRepository.save(clothes);

        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.save(WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(savedClothes)
                .ownershipStatus(OwnershipStatus.WISHLIST)
                .size(request.size())
                .season(request.season())
                .favorite(false)
                .userImageUrl(request.imageUrl())
                .build());

        return ClothesResponse.from(savedClothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse convertToOwned(Long userId, Long clothesId, ClothesConvertToOwnedRequest request) {
        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);

        wardrobeClothes.getClothes().convertToOwned(request.productCode(), request.isVerified());
        wardrobeClothes.convertToOwned(request.size(), request.season(), request.userImageUrl());

        return ClothesResponse.from(wardrobeClothes.getClothes(), wardrobeClothes);
    }

    @Transactional
    public ClothesResponse updateClothes(Long userId, Long clothesId, ClothesUpdateRequest request) {
        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );

        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);
        Clothes clothes = wardrobeClothes.getClothes();

        clothes.update(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                ClothesCategory.fromCode(request.category()),
                request.itemType(),
                request.isVerified()
        );
        clothesTagHelper.replaceColorTags(clothes, request.primaryColor(), request.secondaryColors());
        clothesTagHelper.replaceStyleTags(clothes, request.styles());

        wardrobeClothes.updateWardrobeDetails(
                request.size(),
                request.season(),
                request.imageUrl()
        );

        return ClothesResponse.from(clothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse updateFavorite(Long userId, Long clothesId, ClothesFavoriteRequest request) {
        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);
        wardrobeClothes.updateFavorite(request.isFavorite());
        return ClothesResponse.from(wardrobeClothes.getClothes(), wardrobeClothes);
    }

    /**
     * 사용자 옷장에서 옷을 제거합니다. {@link Clothes} 마스터 데이터는 유지되어
     * 피드·다른 사용자 조회 등 {@code clothes_id} 참조가 계속 동작합니다.
     */
    @Transactional
    public void deleteClothes(Long userId, Long clothesId) {
        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);
        wardrobeClothes.softDelete();
    }

    /** 소유권 확인: 해당 옷이 요청 사용자의 옷장에 없으면 404 */
    private WardrobeClothes getOwnedWardrobeClothes(Long userId, Long clothesId) {
        return wardrobeClothesRepository.findByClothesIdAndUserId(clothesId, userId)
                .orElseThrow(() -> new NoSuchElementException("옷을 찾을 수 없습니다."));
    }

    private Clothes buildClothes(
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            ClothesInfoSource clothesInfoSource,
            String externalSource,
            String externalProductId,
            String externalProductUrl,
            Boolean isVerified,
            String primaryColor,
            List<String> secondaryColors,
            List<String> styles
    ) {
        Clothes clothes = Clothes.builder()
                .name(name)
                .brandName(brandName)
                .productCode(productCode)
                .imageUrl(imageUrl)
                .category(ClothesCategory.fromCode(category))
                .itemType(itemType)
                .clothesInfoSource(clothesInfoSource)
                .externalSource(externalSource)
                .externalProductId(externalProductId)
                .externalProductUrl(externalProductUrl)
                .isVerified(isVerified)
                .build();

        clothesTagHelper.applyColorTags(clothes, primaryColor, secondaryColors);
        clothesTagHelper.applyStyleTags(clothes, styles);
        return clothes;
    }
}
