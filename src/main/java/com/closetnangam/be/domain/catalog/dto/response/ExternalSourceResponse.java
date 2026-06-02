package com.closetnangam.be.domain.catalog.dto.response;

import com.closetnangam.be.domain.catalog.enums.ExternalSource;
import com.closetnangam.be.domain.catalog.enums.ExternalSourceGroup;

public record ExternalSourceResponse(
        String code,
        String label,
        ExternalSourceGroup group,
        String groupLabel,
        boolean allowsCustomInput
) {

    public static ExternalSourceResponse from(ExternalSource source) {
        return new ExternalSourceResponse(
                source.name(),
                source.getLabel(),
                source.getGroup(),
                source.getGroup().getLabel(),
                source.isAllowsCustomInput()
        );
    }
}
