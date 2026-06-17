package com.closetnangam.be.domain.user.dto.request;

import com.closetnangam.be.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(

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

        // 선택 필드 — null 전달 시 기존 값 유지
        String profileImageUrl,
        String profileBio,
        String externalLinkUrl
) {}
