package com.closetnangam.be.domain.clothes.controller;

import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clothes", description = "보유 옷 CRUD API")
@RestController
@RequiredArgsConstructor
public class ClothesController {

    private final ClothesService clothesService;

    @Operation(summary = "보유 옷 목록 조회", description = "사용자 옷장의 보유 옷(OWNED) 목록을 조회합니다.")
    @GetMapping("/api/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getOwnedClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getOwnedClothes(userId)));
    }

    @Operation(summary = "즐겨찾기 옷 목록 조회", description = "즐겨찾기로 표시한 보유 옷 목록을 조회합니다.")
    @GetMapping("/api/users/{userId}/clothes/favorites")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getFavoriteOwnedClothes(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getFavoriteOwnedClothes(userId)));
    }

    @Operation(summary = "옷 상세 조회", description = "옷 이미지, 이름, 브랜드, 카테고리, 타입, 색상, 스타일 정보를 조회합니다.")
    @GetMapping("/api/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> getClothes(@PathVariable Long clothesId) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getClothes(clothesId)));
    }

    @Operation(summary = "보유 옷 등록", description = "사용자 옷장에 보유 옷을 등록합니다.")
    @PostMapping("/api/users/{userId}/clothes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ClothesResponse>> createOwnedClothes(
            @PathVariable Long userId,
            @Valid @RequestBody ClothesCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clothesService.createOwnedClothes(userId, request)));
    }

    @Operation(summary = "옷 즐겨찾기 설정", description = "옷의 즐겨찾기 상태를 등록/해제합니다.")
    @PatchMapping("/api/clothes/{clothesId}/favorite")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateFavorite(
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesFavoriteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateFavorite(clothesId, request)));
    }

    @Operation(summary = "옷 정보 수정", description = "등록된 보유 옷 정보를 수정합니다.")
    @PatchMapping("/api/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateClothes(
            @PathVariable Long clothesId,
            @Valid @RequestBody ClothesUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateClothes(clothesId, request)));
    }

    @Operation(summary = "옷 삭제", description = "등록된 보유 옷을 삭제합니다. 삭제 확인은 프론트에서 처리합니다.")
    @DeleteMapping("/api/clothes/{clothesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteClothes(@PathVariable Long clothesId) {
        clothesService.deleteClothes(clothesId);
        return ResponseEntity.noContent().build();
    }
}
