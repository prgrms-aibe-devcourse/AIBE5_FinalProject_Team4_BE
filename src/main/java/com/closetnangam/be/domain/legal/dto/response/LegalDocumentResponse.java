package com.closetnangam.be.domain.legal.dto.response;

public record LegalDocumentResponse(
        String policyType,
        String version,
        String effectiveDate,
        String lastUpdated,
        String content
) {
}
