package com.closetnangam.be.global.external.clothes.controller;

import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import com.closetnangam.be.global.external.clothes.dto.record.NaverProductCreateRequest;
import com.closetnangam.be.global.external.clothes.dto.response.SaveNaverProductResponse;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @Valid @RequestBody NaverProductCreateRequest request
    ) {
        Long clothesId = externalClothesService.getOrCreateExternalClothes(
                request,
                request.colors(),
                request.styles()
        );

        return ApiResponse.ok(new SaveNaverProductResponse(clothesId));
    }
}
