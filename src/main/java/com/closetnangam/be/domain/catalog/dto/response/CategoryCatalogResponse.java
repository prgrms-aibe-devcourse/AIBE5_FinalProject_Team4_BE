package com.closetnangam.be.domain.catalog.dto.response;

import java.util.List;

public record CategoryCatalogResponse(
        CategoryUsageGuideResponse guide,
        List<CategoryGroupResponse> categories,
        List<StyleResponse> styles,
        List<ColorResponse> colors
) {
}
