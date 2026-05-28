package com.closetnangam.be.domain.clothes.controller;

import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRegistrationMethodsResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.service.ClothesRegistrationService;
import com.closetnangam.be.domain.clothes.service.ClothesService;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clothes", description = "보유 옷 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ClothesController {

    private final ClothesService clothesService;
    private final ClothesRegistrationService clothesRegistrationService;

    @Operation(
            summary = "옷 등록 방식 조회",
            description = "보유 옷 등록 시 선택 가능한 방식(구매내역 기반, 사진 기반) 목록을 반환합니다."
    )
    @GetMapping("/clothes/registration-methods")
    public ResponseEntity<ApiResponse<ClothesRegistrationMethodsResponse>> getRegistrationMethods() {
        return ResponseEntity.ok(ApiResponse.ok(clothesRegistrationService.getRegistrationMethods()));
    }

    // TODO: JWT 인증 구현 후 @PreAuthorize 또는 SecurityContextHolder로 userId 소유권 검증 추가 필요
    @Operation(summary = "보유 옷 목록 조회", description = "사용자 옷장의 보유 옷(OWNED) 목록을 조회합니다.")
    @GetMapping("/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getOwnedClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getOwnedClothes(userId)));
    }

    @Operation(summary = "즐겨찾기 옷 목록 조회", description = "즐겨찾기로 표시한 보유 옷 목록을 조회합니다.")
    @GetMapping("/users/{userId}/clothes/favorites")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getFavoriteOwnedClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getFavoriteOwnedClothes(userId)));
    }

    @Operation(summary = "옷 상세 조회", description = "보유/미보유 옷의 상세 정보(이미지, 이름, 브랜드, 카테고리, 타입, 색상, 스타일)를 조회합니다.")
    @GetMapping("/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> getClothes(@PathVariable Long clothesId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getClothes(clothesId)));
    }

    @Operation(summary = "보유 옷 등록", description = "사용자 옷장에 보유 옷을 등록합니다.")
    @PostMapping("/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<ClothesResponse>> createOwnedClothes(
            @PathVariable Long userId,
            @Valid @RequestBody ClothesCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clothesService.createOwnedClothes(userId, request)));
    }

    @Operation(summary = "옷 즐겨찾기 설정", description = "보유/미보유 옷의 즐겨찾기 상태를 등록/해제합니다.")
    @PatchMapping("/clothes/{clothesId}/favorite")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateFavorite(
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesFavoriteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateFavorite(clothesId, request)));
    }

    @Operation(summary = "옷 정보 수정", description = "등록된 보유/미보유 옷 정보를 수정합니다.")
    @PatchMapping("/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateClothes(
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateClothes(clothesId, request)));
    }

    @Operation(summary = "옷 삭제", description = "등록된 보유/미보유 옷을 삭제합니다. 삭제 확인은 프론트에서 처리합니다.")
    @DeleteMapping("/clothes/{clothesId}")
    public ResponseEntity<Void> deleteClothes(@PathVariable Long clothesId) {
        clothesService.deleteClothes(clothesId);
        return ResponseEntity.noContent().build();
    }
}
