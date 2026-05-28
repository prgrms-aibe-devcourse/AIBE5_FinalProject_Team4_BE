package com.closetnangam.be.domain.catalog.constants;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.catalog.enums.StyleCode;

public final class CatalogLimits {

    /** ClothesColor catalog size minus the required primary color slot. */
    public static final int MAX_SECONDARY_COLORS = 12;

    /** StyleCode catalog size. */
    public static final int MAX_STYLES = 10;

    private CatalogLimits() {
    }

    static {
        int colorCatalogSize = ClothesColor.values().length;
        int styleCatalogSize = StyleCode.values().length;
        if (MAX_SECONDARY_COLORS != colorCatalogSize - 1) {
            throw new ExceptionInInitializerError(
                    "MAX_SECONDARY_COLORS must be ClothesColor count - 1: expected "
                            + (colorCatalogSize - 1));
        }
        if (MAX_STYLES != styleCatalogSize) {
            throw new ExceptionInInitializerError(
                    "MAX_STYLES must match StyleCode count: expected " + styleCatalogSize);
        }
    }
}
