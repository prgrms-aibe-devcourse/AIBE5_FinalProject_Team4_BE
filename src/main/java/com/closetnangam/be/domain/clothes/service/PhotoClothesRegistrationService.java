package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.ai.entity.ClothingAiPhoto;
import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.ai.repository.ClothingAiPhotoRepository;
import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.dto.request.PhotoClothesSaveRequest;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesDraftResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesRegistrationResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoUploadResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.RegistrationSource;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoClothesRegistrationService {

    private static final String EXTERNAL_NONE = "NONE";

    private final ClothingAiPhotoRepository clothingAiPhotoRepository;
    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final StyleRepository styleRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final WardrobeService wardrobeService;
    private final UserRepository userRepository;
    private final LocalImageStorageService localImageStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PhotoUploadResponse uploadPhoto(Long userId, MultipartFile file) {
        User user = getUser(userId);
        LocalImageStorageService.StoredImage storedImage = localImageStorageService.storeClothesPhoto(userId, file);

        ClothingAiPhoto photo = clothingAiPhotoRepository.save(ClothingAiPhoto.builder()
                .user(user)
                .imageUrl(storedImage.publicUrl())
                .storedPath(storedImage.storedPath())
                .originalFilename(storedImage.originalFilename())
                .contentType(storedImage.contentType())
                .analysisStatus(AiAnalysisStatus.UPLOADED)
                .build());

        return new PhotoUploadResponse(
                photo.getId(),
                photo.getImageUrl(),
                photo.getOriginalFilename(),
                photo.getContentType()
        );
    }

    public PhotoClothesDraftResponse getDraft(Long userId, Long photoId) {
        ClothingAiPhoto photo = getOwnedPhoto(userId, photoId);
        return toDraftResponse(photo);
    }

    @Transactional
    public PhotoClothesRegistrationResponse savePhotoClothes(
            Long userId,
            Long photoId,
            PhotoClothesSaveRequest request
    ) {
        validateClassification(request.category(), request.itemType(), request.color(), request.styles());

        ClothingAiPhoto photo = getOwnedPhoto(userId, photoId);
        if (photo.isAlreadySaved()) {
            throw new IllegalStateException("이미 저장된 사진입니다.");
        }

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Clothes clothes = Clothes.builder()
                .wardrobe(wardrobe)
                .name(request.name())
                .brandName(request.brandName())
                .productCode(request.productCode())
                .imageUrl(photo.getImageUrl())
                .category(request.category())
                .itemType(request.itemType())
                .color(request.color())
                .sourceType(SourceType.OWNED)
                .externalSource(EXTERNAL_NONE)
                .externalProductId(EXTERNAL_NONE)
                .externalProductUrl(EXTERNAL_NONE)
                .isVerified(request.isVerified())
                .isFavorite(request.favorite())
                .build();

        applyStyleTags(clothes, request.styles());
        Clothes savedClothes = clothesRepository.save(clothes);

        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.save(WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(savedClothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size(request.size())
                .season(request.season())
                .favorite(request.favorite())
                .registrationSource(RegistrationSource.PHOTO)
                .userImageUrl(photo.getImageUrl())
                .build());

        photo.markSaved(savedClothes.getId(), wardrobeClothes.getId());

        Clothes loadedClothes = clothesRepository.findByIdWithDetails(savedClothes.getId())
                .orElseThrow(() -> new IllegalArgumentException("저장된 옷을 찾을 수 없습니다."));

        return new PhotoClothesRegistrationResponse(
                wardrobeClothes.getId(),
                loadedClothes.getId(),
                photo.getId(),
                wardrobeClothes.getUserImageUrl(),
                wardrobeClothes.getOwnershipStatus(),
                wardrobeClothes.getRegistrationSource(),
                wardrobeClothes.getSize(),
                wardrobeClothes.getSeason(),
                wardrobeClothes.getFavorite(),
                com.closetnangam.be.domain.clothes.dto.response.ClothesResponse.from(loadedClothes)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ClothingAiPhoto getOwnedPhoto(Long userId, Long photoId) {
        return clothingAiPhotoRepository.findByIdAndUser_Id(photoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
    }

    private PhotoClothesDraftResponse toDraftResponse(ClothingAiPhoto photo) {
        List<String> styles = parseStyles(photo.getDraftStylesJson());
        boolean aiFailed = photo.getAnalysisStatus() == AiAnalysisStatus.FAILED;

        return new PhotoClothesDraftResponse(
                photo.getId(),
                photo.getAnalysisStatus(),
                photo.getImageUrl(),
                photo.getFailureMessage(),
                aiFailed,
                photo.getDraftName(),
                photo.getDraftBrandName(),
                photo.getDraftCategory(),
                photo.getDraftItemType(),
                photo.getDraftColor(),
                styles
        );
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
        List<String> uniqueCodes = styleCodes.stream().distinct().toList();
        List<Style> styles = styleRepository.findByCodeIn(uniqueCodes);
        if (styles.size() != uniqueCodes.size()) {
            throw new IllegalArgumentException("존재하지 않는 스타일 코드가 포함되어 있습니다.");
        }
        return styles.stream()
                .map(style -> ClothesStyleTag.create(clothes, style))
                .toList();
    }

    private List<String> parseStyles(String draftStylesJson) {
        if (!StringUtils.hasText(draftStylesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    draftStylesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }
}
