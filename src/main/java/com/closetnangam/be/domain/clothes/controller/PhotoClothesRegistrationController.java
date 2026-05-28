package com.closetnangam.be.domain.clothes.controller;

import com.closetnangam.be.domain.ai.dto.response.AiAnalyzeResponse;
import com.closetnangam.be.domain.ai.service.AiService;
import com.closetnangam.be.domain.clothes.dto.request.PhotoClothesSaveRequest;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesDraftResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesRegistrationResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoUploadResponse;
import com.closetnangam.be.domain.clothes.service.PhotoClothesRegistrationService;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Photo Clothes Registration", description = "사진 기반 보유 옷 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/clothes/photos")
public class PhotoClothesRegistrationController {

    private final PhotoClothesRegistrationService photoClothesRegistrationService;
    private final AiService aiService;

    // TODO: JWT 인증 구현 후 @PreAuthorize 또는 SecurityContextHolder로 userId 소유권 검증 추가 필요
    @Operation(summary = "의류 사진 업로드", description = "jpg, png, webp 형식의 옷 사진을 업로드하고 미리보기 URL을 반환합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhoto(
            @PathVariable Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(photoClothesRegistrationService.uploadPhoto(userId, file)));
    }

    @Operation(summary = "AI 의류 판별", description = "업로드한 사진을 Gemini API로 분석해 카테고리, 타입, 색상, 스타일 임시값을 저장합니다.")
    @PostMapping("/{photoId}/analyze")
    public ResponseEntity<ApiResponse<AiAnalyzeResponse>> analyzePhoto(
            @PathVariable Long userId,
            @PathVariable Long photoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.analyzeClothingPhoto(userId, photoId)));
    }

    @Operation(summary = "AI 판별 임시 결과 조회", description = "AI 판별 결과 또는 실패 안내를 포함한 임시 등록 정보를 조회합니다.")
    @GetMapping("/{photoId}/draft")
    public ResponseEntity<ApiResponse<PhotoClothesDraftResponse>> getDraft(
            @PathVariable Long userId,
            @PathVariable Long photoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(photoClothesRegistrationService.getDraft(userId, photoId)));
    }

    @Operation(summary = "사진 기반 옷 최종 저장", description = "사용자가 수정한 정보와 옷장 전용 정보(size, season 등)를 저장합니다.")
    @PostMapping("/{photoId}/save")
    public ResponseEntity<ApiResponse<PhotoClothesRegistrationResponse>> savePhotoClothes(
            @PathVariable Long userId,
            @PathVariable Long photoId,
            @Valid @RequestBody PhotoClothesSaveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(photoClothesRegistrationService.savePhotoClothes(userId, photoId, request)));
    }
}
