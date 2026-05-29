package com.closetnangam.be.global.external.clothes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverItemRequest {
    private String title;
    private String link;
    private String image;
    private String lprice;
    private String brand;
    private String productId;
    private String category1;
    private String category3;

    public String getCleanTitle() {
        if (this.title == null) {
            return "";
        }
        return this.title.replaceAll("<[^>]*>", "").trim();
    }
}
