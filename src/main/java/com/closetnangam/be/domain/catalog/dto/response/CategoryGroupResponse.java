package com.closetnangam.be.domain.catalog.dto.response;

public record CategoryGroupResponse(
        String code,
        String name,
        java.util.List<ItemTypeResponse> itemTypes
) {
}
