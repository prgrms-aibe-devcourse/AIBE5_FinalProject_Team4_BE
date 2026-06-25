package com.closetnangam.be.domain.purchase.controller;

import com.closetnangam.be.domain.purchase.dto.request.PurchaseCaptureSaveRequest;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureAnalyzeResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureDraftResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureRegistrationResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureUploadResponse;
import com.closetnangam.be.domain.purchase.service.PurchaseCaptureAiService;
import com.closetnangam.be.domain.purchase.service.PurchaseCaptureRegistrationService;
import com.closetnangam.be.global.common.response.ApiResponse;
import com.closetnangam.be.global.common.util.SecurityUtils;
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

@Tag(name = "Purchase Capture Registration", description = "구매내역 캡처 기반 보유 옷 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/clothes/purchase-captures")
public class PurchaseCaptureRegistrationController {

    private final PurchaseCaptureRegistrationService purchaseCaptureRegistrationService;
    private final PurchaseCaptureAiService purchaseCaptureAiService;

    @Operation(summary = "구매내역 캡처 업로드", description = "구매내역 스크린샷을 업로드하고 미리보기 URL을 반환합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PurchaseCaptureUploadResponse>> uploadCapture(
            @PathVariable Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        SecurityUtils.verifyOwnership(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(purchaseCaptureRegistrationService.uploadCapture(userId, file)));
    }

    @Operation(summary = "구매내역 AI 추출", description = "캡처 이미지에서 상품명·옵션·카테고리 등을 OCR/추출합니다.")
    @PostMapping("/{captureId}/analyze")
    public ResponseEntity<ApiResponse<PurchaseCaptureAnalyzeResponse>> analyzeCapture(
            @PathVariable Long userId,
            @PathVariable Long captureId
    ) {
        SecurityUtils.verifyOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok(purchaseCaptureAiService.analyzePurchaseCapture(userId, captureId)));
    }

    @Operation(summary = "구매내역 추출 임시 결과 조회", description = "AI 추출 결과 또는 실패 안내를 포함한 임시 등록 정보를 조회합니다.")
    @GetMapping("/{captureId}/draft")
    public ResponseEntity<ApiResponse<PurchaseCaptureDraftResponse>> getDraft(
            @PathVariable Long userId,
            @PathVariable Long captureId
    ) {
        SecurityUtils.verifyOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok(purchaseCaptureRegistrationService.getDraft(userId, captureId)));
    }

    @Operation(
            summary = "구매내역 기반 옷 최종 저장",
            description = "사용자가 수정한 옷 공통 정보, 외부 출처(externalSource), 옷장 전용 정보(size 등)를 저장합니다."
    )
    @PostMapping("/{captureId}/save")
    public ResponseEntity<ApiResponse<PurchaseCaptureRegistrationResponse>> savePurchaseCaptureClothes(
            @PathVariable Long userId,
            @PathVariable Long captureId,
            @Valid @RequestBody PurchaseCaptureSaveRequest request
    ) {
        SecurityUtils.verifyOwnership(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(purchaseCaptureRegistrationService.savePurchaseCaptureClothes(userId, captureId, request)));
    }

    @Operation(summary = "구매내역 상품 건너뛰기", description = "복수 상품 캡처에서 특정 itemIndex를 저장하지 않고 건너뜁니다.")
    @PostMapping("/{captureId}/items/{itemIndex}/skip")
    public ResponseEntity<ApiResponse<PurchaseCaptureDraftResponse>> skipPurchaseCaptureItem(
            @PathVariable Long userId,
            @PathVariable Long captureId,
            @PathVariable int itemIndex
    ) {
        SecurityUtils.verifyOwnership(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                purchaseCaptureRegistrationService.skipPurchaseCaptureItem(userId, captureId, itemIndex)
        ));
    }
}
