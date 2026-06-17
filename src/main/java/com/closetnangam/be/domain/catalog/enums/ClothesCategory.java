package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClothesCategory {

    TOP("상의"),
    BOTTOM("하의"),
    OUTER("아우터"),
    SHOES("신발");

    private final String label;

    public static ClothesCategory fromCode(String code) {
        if (code == null || code.isBlank()) return TOP;
        try {
            return ClothesCategory.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return TOP;
        }
    }
}
