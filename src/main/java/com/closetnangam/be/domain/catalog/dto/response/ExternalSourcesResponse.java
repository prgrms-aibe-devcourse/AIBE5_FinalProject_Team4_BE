package com.closetnangam.be.domain.catalog.dto.response;

import java.util.List;

public record ExternalSourcesResponse(
        String description,
        List<ExternalSourceResponse> sources
) {
}
