package com.closetnangam.be.global.external.clothes.controller;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
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
        // 1. 테스트용 유저 ID 1번 강제 주입 (로그인 구현 전까지 유지)
        Long authenticatedUserId = 1L;

//        로그인 기능 구현 하면 다시 쓸 예정
//        Long authenticatedUserId = SecurityUtils.getCurrentUserId();

        // 메서드명을 새로 바꾼 getOrCreateExternalClothes로 매핑하고, DTO 내부의 colors(), styles()를 넘깁니다.
        Long clothesId = externalClothesService.getOrCreateExternalClothes(
                request, request.colors(), request.styles());

        // 3. [옷장 담당 역할] 유저의 옷장에 이 옷을 담아두는 로직 호출
        // 임시로 주석 처리해 두었다가, 옷장 담당 팀원이 메서드(예: addClothesToWishlist)를 만들어주면 주석을 풀고 연결합니다!
        // Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(authenticatedUserId);
        // wardrobeService.addClothesToWishlist(authenticatedUserId, wardrobe, clothesId);

        return ApiResponse.ok(new SaveNaverProductResponse(clothesId));
    }
}
