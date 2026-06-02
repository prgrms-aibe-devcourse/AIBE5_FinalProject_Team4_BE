package com.closetnangam.be.domain.clothes.controller;

import com.closetnangam.be.domain.clothes.dto.request.ClothesCreateRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesFavoriteRequest;
import com.closetnangam.be.domain.clothes.dto.request.ClothesUpdateRequest;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRegistrationMethodsResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.service.ClothesRecommendationService;
import com.closetnangam.be.domain.clothes.service.ClothesRegistrationService;
import com.closetnangam.be.domain.clothes.service.ClothesService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clothes", description = "보유 옷 CRUD API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ClothesController {

    private final ClothesService clothesService;
    private final ClothesRegistrationService clothesRegistrationService;
    private final ClothesRecommendationService clothesRecommendationService;

    @Operation(
            summary = "옷 등록 방식 조회",
            description = "보유 옷 등록 시 선택 가능한 방식(구매내역 기반, 사진 기반) 목록을 반환합니다."
    )
    @GetMapping("/clothes/registration-methods")
    public ResponseEntity<ApiResponse<ClothesRegistrationMethodsResponse>> getRegistrationMethods() {
        return ResponseEntity.ok(ApiResponse.ok(clothesRegistrationService.getRegistrationMethods()));
    }

    @Operation(summary = "보유 옷 목록 조회", description = "사용자 옷장의 보유 옷(OWNED) 목록을 조회합니다.")
    @GetMapping("/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getOwnedClothes(
            @PathVariable @Min(1) Long userId
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getOwnedClothes(userId)));
    }

    @Operation(summary = "즐겨찾기 옷 목록 조회", description = "즐겨찾기로 표시한 보유 옷 목록을 조회합니다.")
    @GetMapping("/users/{userId}/clothes/favorites")
    public ResponseEntity<ApiResponse<List<ClothesResponse>>> getFavoriteOwnedClothes(
            @PathVariable @Min(1) Long userId
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getFavoriteOwnedClothes(userId)));
    }

    @Operation(summary = "옷 상세 조회", description = "보유/미보유 옷의 상세 정보(이미지, 이름, 브랜드, 카테고리, 타입, 색상, 스타일)를 조회합니다.")
    @GetMapping("/users/{userId}/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> getClothes(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long clothesId
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(clothesService.getClothes(userId, clothesId)));
    }

    @Operation(summary = "보유 옷 등록", description = "사용자 옷장에 보유 옷을 등록합니다.")
    @PostMapping("/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<ClothesResponse>> createOwnedClothes(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody ClothesCreateRequest request
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clothesService.createOwnedClothes(userId, request)));
    }

    @Operation(summary = "옷 즐겨찾기 설정", description = "보유/미보유 옷의 즐겨찾기 상태를 등록/해제합니다.")
    @PatchMapping("/users/{userId}/clothes/{clothesId}/favorite")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateFavorite(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long clothesId,
            @Valid @RequestBody ClothesFavoriteRequest request
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateFavorite(userId, clothesId, request)));
    }

    @Operation(summary = "옷 정보 수정", description = "등록된 보유/미보유 옷 정보를 수정합니다.")
    @PatchMapping("/users/{userId}/clothes/{clothesId}")
    public ResponseEntity<ApiResponse<ClothesResponse>> updateClothes(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long clothesId,
            @Valid @RequestBody ClothesUpdateRequest request
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(clothesService.updateClothes(userId, clothesId, request)));
    }

    @Operation(summary = "옷 삭제", description = "등록된 보유/미보유 옷을 삭제합니다. 삭제 확인은 프론트에서 처리합니다.")
    @DeleteMapping("/users/{userId}/clothes/{clothesId}")
    public ResponseEntity<Void> deleteClothes(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long clothesId
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        clothesService.deleteClothes(userId, clothesId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "어울리는 옷 추천",
            description = """
                    보유 옷 1벌을 기준으로 같은 카테고리를 제외한 나머지 보유 옷들을 점수화해 카테고리별로 추천합니다.
                    - 색상(35%): 색상 조합 차트 + 양방향, secondary 0.6 가중
                    - 스타일(30%): 기준 PRIMARY 스타일이 후보 PRIMARY·SECONDARY에 포함되면 1.0, 아니면 0.2
                    - itemType(20%): 소분류 cohesion group + 대표 조합 페어
                    - 시즌(15%): 동일 1.0 / 한쪽 미입력 0.7 / 불일치 0.3
                    - limitPerCategory: 카테고리당 최대 추천 수 (기본 5, 최대 10)
                    """
    )
    @GetMapping("/users/{userId}/clothes/{clothesId}/recommendations")
    public ResponseEntity<ApiResponse<ClothesRecommendationResponse>> getRecommendations(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long clothesId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limitPerCategory
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                clothesRecommendationService.recommend(userId, clothesId, limitPerCategory)
        ));
    }
}
