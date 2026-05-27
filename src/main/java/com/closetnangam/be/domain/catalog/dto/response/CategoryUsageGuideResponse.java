package com.closetnangam.be.domain.catalog.dto.response;

import java.util.List;
import java.util.Map;

public record CategoryUsageGuideResponse(
        String summary,
        List<GuideFieldResponse> fields,
        List<String> notes,
        Map<String, Object> example
) {
}
