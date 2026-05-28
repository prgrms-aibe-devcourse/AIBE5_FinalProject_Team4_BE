package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.clothes.dto.request.ClothesConvertToOwnedRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.request.WishlistClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.RegistrationSource;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesService {

    private static final String EXTERNAL_NONE = "NONE";

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

    public ClothesResponse getClothes(Long clothesId) {
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdWithDetails(clothesId).orElse(null);
        Clothes clothes = wardrobeClothes != null
                ? wardrobeClothes.getClothes()
                : getClothesWithDetails(clothesId);
        return ClothesResponse.from(clothes, wardrobeClothes);
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
                SourceType.OWNED,
                EXTERNAL_NONE,
                EXTERNAL_NONE,
                EXTERNAL_NONE,
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
                .registrationSource(RegistrationSource.MANUAL)
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

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        Clothes clothes = buildClothes(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                SourceType.WISHLIST,
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
                .registrationSource(RegistrationSource.MANUAL)
                .userImageUrl(request.imageUrl())
                .build());

        return ClothesResponse.from(savedClothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse convertToOwned(Long clothesId, ClothesConvertToOwnedRequest request) {
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdWithDetails(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("옷장 등록 정보를 찾을 수 없습니다."));

        wardrobeClothes.getClothes().convertToOwned(request.productCode(), request.isVerified());
        wardrobeClothes.convertToOwned(request.size(), request.season(), request.userImageUrl());

        return ClothesResponse.from(wardrobeClothes.getClothes(), wardrobeClothes);
    }

    @Transactional
    public ClothesResponse updateClothes(Long clothesId, ClothesUpdateRequest request) {
        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );

        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdWithDetails(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("옷장 등록 정보를 찾을 수 없습니다."));
        Clothes clothes = wardrobeClothes.getClothes();

        clothes.update(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
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
    public ClothesResponse updateFavorite(Long clothesId, ClothesFavoriteRequest request) {
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdWithDetails(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("옷장 등록 정보를 찾을 수 없습니다."));
        wardrobeClothes.updateFavorite(request.isFavorite());
        return ClothesResponse.from(wardrobeClothes.getClothes(), wardrobeClothes);
    }

    @Transactional(readOnly = false)
    public void deleteClothes(Long clothesId) {
        getClothesWithDetails(clothesId);
        wardrobeClothesRepository.deleteByClothes_Id(clothesId);
        clothesRepository.deleteById(clothesId);
    }

    private Clothes getClothesWithDetails(Long clothesId) {
        return clothesRepository.findById(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("옷을 찾을 수 없습니다."));
    }

    private Clothes buildClothes(
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            SourceType sourceType,
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
                .category(category)
                .itemType(itemType)
                .sourceType(sourceType)
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
