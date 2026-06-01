package com.closetnangam.be.global.external.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverItemRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 255, message = "상품명은 255자 이하여야 합니다")
    private String title;

    @NotBlank(message = "상품 링크는 필수입니다")
    @Size(max = 500, message = "링크는 500자 이하여야 합니다")
    private String link;

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다")
    private String image;

    @Size(max = 100)
    private String brand;

    @NotBlank(message = "상품 ID는 필수입니다")
    private String productId;

    @Size(max = 100)
    private String category1;

    @Size(max = 100)
    private String category3;

    public String getCleanTitle() {
        if (this.title == null) {
            return "";
        }
        return this.title.replaceAll("<[^>]*>", "").trim();
    }
}