package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalSourceGroup {

    OPEN_MARKET("플랫폼"),
    FASHION_PLATFORM("플랫폼 및 SPA"),
    SPA("SPA"),
    CUSTOM("직접입력");

    private final String label;
}
