package com.closetnangam.be.domain.clothes.controller;

import com.closetnangam.be.domain.clothes.dto.request.ClothesConvertToOwnedRequest;
import com.closetnangam.be.domain.clothes.dto.request.WishlistClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.service.ClothesService;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Wishlist Clothes", description = "미보유 옷(WISHLIST) CRUD API")
@RestController
@RequiredArgsConstructor
public class WishlistClothesController {

    private final ClothesService clothesService;

    // TODO: JWT 인증 구현 후 @PreAuthorize 또는 SecurityContextHolder로 userId 소유권 검증 추가 필요
    @Operation(summary = "미보유 옷 목록 조회", description = "추천받아 저장한 옷, 관심 상품(WISHLIST) 목록을 조회합니다.")
    @GetMapping("/api/users/{userId}/wishlist-clothes")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getWishlistClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getWishlistClothes(userId)));
    }

    @Operation(summary = "미보유 옷 즐겨찾기 목록 조회", description = "즐겨찾기로 표시한 미보유 옷 목록을 조회합니다.")
    @GetMapping("/api/users/{userId}/wishlist-clothes/favorites")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getFavoriteWishlistClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getFavoriteWishlistClothes(userId)));
    }

    @Operation(summary = "미보유 옷 등록", description = "추천 상품 또는 외부 쇼핑 상품을 미보유 옷으로 저장합니다.")
    @PostMapping("/api/users/{userId}/wishlist-clothes")
    public ResponseEntity<ApiResponse<ClothesResponse>> createWishlistClothes(
            @PathVariable Long userId,
            @Valid @RequestBody WishlistClothesCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clothesService.createWishlistClothes(userId, request)));
    }

    // TODO: JWT 인증 구현 후 SecurityContextHolder로 clothesId 소유권 검증 추가 필요
    @Operation(summary = "미보유 → 보유 전환", description = "구매 후 미보유 옷을 보유 옷(OWNED)으로 전환합니다.")
    @PatchMapping("/api/clothes/{clothesId}/convert-to-owned")
    public ResponseEntity<ApiResponse<ClothesResponse>> convertToOwned(
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesConvertToOwnedRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.convertToOwned(clothesId, request)));
    }
}
