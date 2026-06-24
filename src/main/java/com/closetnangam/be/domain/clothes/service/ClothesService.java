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
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.helper.WardrobeExclusionMatcher;
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
    private final WardrobeExclusionMatcher wardrobeExclusionMatcher;
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
                request.styles(),
                request.gender()
        );
        clothesTagHelper.validateSeasonIfPresent(request.season());

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        Clothes clothes = buildClothes(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                request.gender(),
                request.season(),
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
                request.styles(),
                request.gender()
        );
        clothesTagHelper.validateExternalSource(request.externalSource());
        clothesTagHelper.validateSeasonIfPresent(request.season());

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        Clothes clothes = buildClothes(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                request.gender(),
                request.season(),
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
                .favorite(false)
                .userImageUrl(request.imageUrl())
                .build());

        return ClothesResponse.from(savedClothes, wardrobeClothes);
    }

    /**
     * 추천 후보·피드 등 이미 {@link Clothes} 마스터에 존재하는 옷을 사용자 위시리스트에 연결합니다.
     * {@link ClothesInfoSource#EXTERNAL_SHOPPING} 공용 마스터와 {@link ClothesInfoSource#PHOTO} 기반 옷을 허용합니다.
     * 구매내역({@link ClothesInfoSource#PURCHASE_HISTORY}) 기반 개인 데이터는 타 사용자 메타데이터 노출 방지를 위해 차단합니다.
     */
    @Transactional
    public ClothesResponse addExistingClothesToWishlist(Long userId, Long clothesId) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .filter(c -> c.getClothesInfoSource() != ClothesInfoSource.PURCHASE_HISTORY)
                .orElseThrow(() -> new NoSuchElementException("옷을 찾을 수 없습니다."));

        var existingLink = wardrobeClothesRepository.findByClothesIdAndUserIdIgnoringSoftDelete(clothesId, userId);
        if (existingLink.isPresent()) {
            WardrobeClothes wardrobeClothes = existingLink.get();
            if (!wardrobeClothes.isDeleted()) {
                if (wardrobeClothes.getOwnershipStatus() == OwnershipStatus.WISHLIST) {
                    throw new IllegalStateException("이미 위시리스트에 등록된 옷입니다.");
                }
                throw new IllegalStateException("이미 보유 중인 옷입니다.");
            }
            wardrobeClothes.restoreAsWishlist(clothes.getImageUrl(), null);
            return ClothesResponse.from(clothes, wardrobeClothes);
        }

        wardrobeExclusionMatcher.rejectIfEquivalentAlreadyInWardrobe(userId, clothes);

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.save(WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.WISHLIST)
                .size("FREE")
                .favorite(false)
                .userImageUrl(clothes.getImageUrl())
                .registrationSource(clothes.getClothesInfoSource())
                .build());

        return ClothesResponse.from(clothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse convertToOwned(Long userId, Long clothesId, ClothesConvertToOwnedRequest request) {
        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);
        Clothes linkedClothes = wardrobeClothes.getClothes();
        ClothesInfoSource originalInfoSource = linkedClothes.getClothesInfoSource();

        Clothes ownedClothes = linkedClothes;
        if (originalInfoSource == ClothesInfoSource.EXTERNAL_SHOPPING) {
            ownedClothes = cloneExternalShoppingAsOwned(linkedClothes, request.productCode(), request.isVerified());
            wardrobeClothes.relinkClothes(ownedClothes);
        } else {
            linkedClothes.convertToOwned(request.productCode(), request.isVerified());
        }

        ClothesInfoSource ownedRegistrationSource = originalInfoSource;
        wardrobeClothes.convertToOwned(
                request.size(),
                request.userImageUrl(),
                ownedRegistrationSource
        );

        return ClothesResponse.from(ownedClothes, wardrobeClothes);
    }

    @Transactional
    public ClothesResponse updateClothes(Long userId, Long clothesId, ClothesUpdateRequest request) {
        WardrobeClothes wardrobeClothes = getOwnedWardrobeClothes(userId, clothesId);
        Clothes clothes = wardrobeClothes.getClothes();

        if (isExternalCatalogGarment(wardrobeClothes, clothes)) {
            wardrobeClothes.updateWardrobeDetails(
                    request.size(),
                    wardrobeClothes.getUserImageUrl()
            );
            return ClothesResponse.from(clothes, wardrobeClothes);
        }

        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles(),
                request.gender()
        );
        clothesTagHelper.validateSeasonIfPresent(request.season());

        clothes.update(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                ClothesGender.fromCode(request.gender()),
                ClothesSeason.fromCodeOrDefault(request.season()),
                request.isVerified()
        );
        clothesTagHelper.replaceColorTags(clothes, request.primaryColor(), request.secondaryColors());
        clothesTagHelper.replaceStyleTags(clothes, request.styles());

        wardrobeClothes.updateWardrobeDetails(
                request.size(),
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

    /**
     * 외부 쇼핑 카탈로그에서 가져온 옷 — 공통 옷 정보는 유지하고 옷장 사이즈만 변경합니다.
     */
    private boolean isExternalCatalogGarment(WardrobeClothes wardrobeClothes, Clothes clothes) {
        return wardrobeClothes.getRegistrationSource() == ClothesInfoSource.EXTERNAL_SHOPPING
                || clothes.getClothesInfoSource() == ClothesInfoSource.EXTERNAL_SHOPPING;
    }

    private Clothes buildClothes(
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            String gender,
            String season,
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
                .category(category)
                .itemType(itemType)
                .gender(ClothesGender.fromCode(gender))
                .season(ClothesSeason.fromCodeOrDefault(season))
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

    /**
     * 공용 {@link ClothesInfoSource#EXTERNAL_SHOPPING} 마스터는 그대로 두고,
     * 사용자 보유 전환용 {@link ClothesInfoSource#PURCHASE_HISTORY} 행을 새로 만듭니다.
     */
    private Clothes cloneExternalShoppingAsOwned(Clothes source, String productCode, Boolean isVerified) {
        Clothes owned = Clothes.builder()
                .name(source.getName())
                .brandName(source.getBrandName())
                .productCode(productCode)
                .imageUrl(source.getImageUrl())
                .category(source.getCategory())
                .itemType(source.getItemType())
                .gender(source.getGender())
                .season(source.getSeason())
                .clothesInfoSource(ClothesInfoSource.PURCHASE_HISTORY)
                .externalSource(Clothes.EXTERNAL_NONE)
                .externalProductId(Clothes.EXTERNAL_NONE)
                .externalProductUrl(Clothes.EXTERNAL_NONE)
                .isVerified(isVerified)
                .build();
        clothesTagHelper.copyTagsFrom(source, owned);
        return clothesRepository.save(owned);
    }
}
