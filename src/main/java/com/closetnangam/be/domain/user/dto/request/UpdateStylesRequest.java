package com.closetnangam.be.domain.user.dto.request;

import com.closetnangam.be.domain.catalog.constants.CatalogLimits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateStylesRequest(

        @NotEmpty(message = "스타일을 1개 이상 선택해주세요.")
        @Size(max = CatalogLimits.MAX_STYLES, message = "스타일은 최대 10개까지 선택할 수 있습니다.")
        List<String> styleCodes
) {}
