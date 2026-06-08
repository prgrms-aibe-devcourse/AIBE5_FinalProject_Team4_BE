package com.closetnangam.be.domain.recommendation.dto.request;

import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AI MD가 추천한 코디 후보 중 사용자가 저장하기로 선택한 1개 코디.
 *
 * 추천 조회 단계에서는 DB에 저장하지 않기 때문에, 프론트는 선택된 후보의 보유 옷 ID와 외부 상품 정보를
 * 이 요청으로 다시 전달해야 한다.
 */
public record AiMdOutfitSaveRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String description,
        @Size(max = 50) String situation,
        @Size(max = 50) String season,
        String reason,
        String stylingTip,
        @NotEmpty List<Long> wardrobeClothesIds,
        List<@Valid NaverShoppingProductResponse> externalProducts
) {
}
