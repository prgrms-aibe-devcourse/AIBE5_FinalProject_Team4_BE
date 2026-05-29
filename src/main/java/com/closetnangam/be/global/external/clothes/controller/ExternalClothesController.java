package com.closetnangam.be.global.external.clothes.controller;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.common.response.ApiResponse;
import com.closetnangam.be.global.external.clothes.dto.NaverItemRequest;
import com.closetnangam.be.global.external.clothes.dto.record.SaveNaverProductRequest;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import com.closetnangam.be.global.external.clothes.dto.response.SaveNaverProductResponse;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "External Clothes", description = "외부 서비스 상품 연동 API")
@RestController
@RequestMapping("/api/v1/external/clothes")
@RequiredArgsConstructor
public class ExternalClothesController {

    private final ExternalClothesService externalClothesService;
    private final WardrobeService wardrobeService;

    @Operation(summary = "네이버 상품 저장", description = "네이버에서 검색한 상품 정보를 저장합니다.")
    @PostMapping("/naver")
    public ApiResponse<SaveNaverProductResponse> saveNaverProduct(
            @RequestParam Long userId,
            @Valid @RequestBody SaveNaverProductRequest request
    ) {
        // TODO: SecurityContextHolder 통합 후 아래 검증 추가
        // Long authenticatedUserId = SecurityUtils.getCurrentUserId();
        // if (!userId.equals(authenticatedUserId)) {
        //     throw new IllegalArgumentException("자신의 옷장에만 접근할 수 있습니다");
        // }

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);
        // request 내부에서 필요한 객체들만 쏙쏙 뽑아서 전달
        Long clothesId = externalClothesService.saveNaverToWishlist(
                userId,
                wardrobe,
                request, // <-- request 자체가 이제 NaverItemRequest 역할을 함!
                request.colors(),
                request.styles()
        );

        return ApiResponse.ok(new SaveNaverProductResponse(clothesId));
    }
}
