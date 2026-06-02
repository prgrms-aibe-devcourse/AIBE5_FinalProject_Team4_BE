package com.closetnangam.be.domain.outfit.controller;

import com.closetnangam.be.domain.outfit.dto.request.OutfitCreateRequest;
import com.closetnangam.be.domain.outfit.dto.response.OutfitBookResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.domain.outfit.service.OutfitService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Outfit", description = "코디북 API")
@RestController
@RequestMapping("/api/v1/outfit-books")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @Operation(summary = "코디북 생성", description = "현재 로그인한 사용자의 코디북을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<OutfitBookResponse>> createBook() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(outfitService.createBook(userId)));
    }

    @Operation(summary = "코디북 조회", description = "현재 로그인한 사용자의 코디북과 코디 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<OutfitBookResponse>> getMyBook() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(outfitService.getBookByUserId(userId)));
    }

    @Operation(summary = "코디북 상세 조회", description = "코디북 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<OutfitBookResponse>> getBookById(@PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(outfitService.getBookById(bookId, userId)));
    }

    @Operation(summary = "코디 등록", description = "코디북에 새로운 코디를 추가합니다.")
    @PostMapping("/{bookId}/outfits")
    public ResponseEntity<ApiResponse<OutfitResponse>> addOutfit(
            @PathVariable Long bookId,
            @Valid @RequestBody OutfitCreateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(outfitService.createOutfit(bookId, userId, request)));
    }
}
