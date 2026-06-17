package com.closetnangam.be.domain.user.dto.request;

import com.closetnangam.be.domain.catalog.constants.CatalogLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateStylesRequest(

        @NotNull(message = "스타일 목록은 필수입니다.")
        @Size(min = 2, max = CatalogLimits.MAX_STYLES, message = "스타일은 2개 이상 10개 이하로 선택해주세요.")
        List<String> styleCodes
) {}
