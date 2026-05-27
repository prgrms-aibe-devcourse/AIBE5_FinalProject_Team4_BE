package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
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
    private final StyleRepository styleRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final WardrobeService wardrobeService;

    public List<ClothesResponse> getOwnedClothes(Long userId) {
        return clothesRepository.findAllByUserIdAndSourceType(userId, SourceType.OWNED).stream()
                .map(ClothesResponse::from)
                .toList();
    }

    public List<ClothesResponse> getFavoriteOwnedClothes(Long userId) {
        return clothesRepository.findFavoritesByUserIdAndSourceType(userId, SourceType.OWNED).stream()
                .map(ClothesResponse::from)
                .toList();
    }

    public ClothesResponse getClothes(Long clothesId) {
        Clothes clothes = getClothesWithDetails(clothesId);
        return ClothesResponse.from(clothes);
    }

    @Transactional
    public ClothesResponse createOwnedClothes(Long userId, ClothesCreateRequest request) {
        validateClassification(request.category(), request.itemType(), request.color(), request.styles());

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Clothes clothes = Clothes.builder()
                .wardrobe(wardrobe)
                .name(request.name())
                .brandName(request.brandName())
                .productCode(request.productCode())
                .imageUrl(request.imageUrl())
                .category(request.category())
                .itemType(request.itemType())
                .color(request.color())
                .sourceType(SourceType.OWNED)
                .externalSource(EXTERNAL_NONE)
                .externalProductId(EXTERNAL_NONE)
                .externalProductUrl(EXTERNAL_NONE)
                .isVerified(request.isVerified())
                .isFavorite(false)
                .build();

        applyStyleTags(clothes, request.styles());
        Clothes saved = clothesRepository.save(clothes);
        return ClothesResponse.from(saved);
    }

    @Transactional
    public ClothesResponse updateClothes(Long clothesId, ClothesUpdateRequest request) {
        validateClassification(request.category(), request.itemType(), request.color(), request.styles());

        Clothes clothes = getClothesWithDetails(clothesId);

        clothes.update(
                request.name(),
                request.brandName(),
                request.productCode(),
                request.imageUrl(),
                request.category(),
                request.itemType(),
                request.color(),
                request.isVerified()
        );

        clothes.replaceStyleTags(buildStyleTags(clothes, request.styles()));
        return ClothesResponse.from(clothes);
    }

    @Transactional
    public ClothesResponse updateFavorite(Long clothesId, ClothesFavoriteRequest request) {
        Clothes clothes = getClothesWithDetails(clothesId);
        clothes.updateFavorite(request.isFavorite());
        return ClothesResponse.from(clothes);
    }

    @Transactional
    public void deleteClothes(Long clothesId) {
        Clothes clothes = getClothesWithDetails(clothesId);
        clothesRepository.delete(clothes);
    }

    private Clothes getClothesWithDetails(Long clothesId) {
        return clothesRepository.findByIdWithDetails(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("옷을 찾을 수 없습니다."));
    }

    private void validateClassification(
            String category,
            String itemType,
            String color,
            List<String> styles
    ) {
        categoryCatalogService.validateClothesClassification(category, itemType, color);
        categoryCatalogService.validateStyleCodes(styles);
    }

    private void applyStyleTags(Clothes clothes, List<String> styleCodes) {
        buildStyleTags(clothes, styleCodes).forEach(clothes::addStyleTag);
    }

    private List<ClothesStyleTag> buildStyleTags(Clothes clothes, List<String> styleCodes) {
        return styleCodes.stream()
                .map(styleRepository::findByCode)
                .map(optionalStyle -> optionalStyle.orElseThrow(
                        () -> new IllegalArgumentException("존재하지 않는 스타일 코드입니다.")
                ))
                .map(style -> ClothesStyleTag.create(clothes, style))
                .toList();
    }
}
