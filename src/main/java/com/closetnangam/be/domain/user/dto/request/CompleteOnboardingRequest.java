package com.closetnangam.be.domain.user.dto.request;

import com.closetnangam.be.domain.catalog.constants.CatalogLimits;
import com.closetnangam.be.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CompleteOnboardingRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하로 입력해주세요.")
        String nickname,

        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birthDate,

        @NotNull(message = "성별은 필수입니다.")
        User.Gender gender,

        @NotBlank(message = "지역명은 필수입니다.")
        String regionName,

        @NotBlank(message = "지역 코드는 필수입니다.")
        String regionCode,

        @NotNull(message = "스타일 목록은 필수입니다.")
        @Size(min = 2, max = CatalogLimits.MAX_STYLES, message = "스타일은 2개 이상 10개 이하로 선택해주세요.")
        List<String> styleCodes,

        @NotNull(message = "마케팅 동의 여부는 필수입니다.")
        Boolean marketingAgreed
) {
}
