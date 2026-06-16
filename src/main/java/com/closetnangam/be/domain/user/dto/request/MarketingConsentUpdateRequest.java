package com.closetnangam.be.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record MarketingConsentUpdateRequest(
        @NotNull Boolean marketingAgreed
) {
}
