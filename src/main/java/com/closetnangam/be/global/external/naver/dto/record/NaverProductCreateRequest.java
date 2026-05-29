package com.closetnangam.be.global.external.naver.dto.record;


import com.closetnangam.be.global.external.naver.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.naver.dto.request.ClothingColorDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NaverProductCreateRequest(
        @NotBlank(message = "상품 ID는 필수입니다.")
        String productId,

        String brand,

        String category3,

        @JsonProperty("title")
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 255, message = "상품명은 255자 이내여야 합니다.")
        String cleanTitle,

        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 500, message = "이미지 URL이 너무 깁니다.")
        String image,

        @NotBlank(message = "상품 링크는 필수입니다.")
        @Size(max = 500, message = "링크 주소가 너무 깁니다.")
        String link,

        @JsonProperty("colors")
        @Valid
        List<ClothingColorDto> colors,

        @JsonProperty("styles")
        @Valid
        List<ClothesStyleDto> styles
) {}