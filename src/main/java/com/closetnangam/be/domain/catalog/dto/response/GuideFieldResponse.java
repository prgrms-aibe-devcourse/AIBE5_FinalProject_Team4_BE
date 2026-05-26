package com.closetnangam.be.domain.catalog.dto.response;

public record GuideFieldResponse(
        String key,
        String label,
        String description,
        String example
) {
}
