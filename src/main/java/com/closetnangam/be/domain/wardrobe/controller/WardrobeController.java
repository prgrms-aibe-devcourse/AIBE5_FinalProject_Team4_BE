package com.closetnangam.be.domain.wardrobe.controller;

import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeResponse;
import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeStatisticsResponse;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.domain.wardrobe.service.WardrobeStatisticsService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wardrobe", description = "옷장 API")
@RestController
@Validated
@RequestMapping("/api/v1/wardrobes")
@RequiredArgsConstructor
public class WardrobeController {

    private final WardrobeService wardrobeService;
    private final WardrobeStatisticsService wardrobeStatisticsService;

    @Operation(summary = "회원 옷장 조회", description = "회원당 1개의 옷장을 조회합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<WardrobeResponse>> getWardrobeByUserId(@PathVariable @Min(1) Long userId) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(wardrobeService.getWardrobeByUserId(userId)));
    }

    @Operation(summary = "회원 옷장 생성", description = "회원 가입 후 옷장을 1회 생성합니다.")
    @PostMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<WardrobeResponse>> createWardrobe(@PathVariable @Min(1) Long userId) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(wardrobeService.createWardrobe(userId)));
    }

    @Operation(
            summary = "옷장 통계 조회",
            description = """
                    옷장에 등록된 보유/미보유 옷 기준 통계와 스타일 옷장 가중치를 반환합니다.
                    - totalOwnedCount: 보유 옷 개수
                    - totalWishlistCount: 미보유 옷 개수
                    - totalWardrobeClothesCount: 보유/미보유 전체 옷장 등록 개수
                    - itemTypes: itemType별 옷장 등록 개수
                    - userStylePayloads: USER_STYLES.wardrobe_weight 반영용 값
                      (PRIMARY 태그 0.7, SECONDARY 태그 0.3 합산 후 0~100 정규화)
                    preference_weight, feedback_weight, combined_weight는 미구현 상태입니다.
                    """
    )
    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<ApiResponse<WardrobeStatisticsResponse>> getWardrobeStatistics(@PathVariable @Min(1) Long userId) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(wardrobeStatisticsService.getStatistics(userId)));
    }
}
