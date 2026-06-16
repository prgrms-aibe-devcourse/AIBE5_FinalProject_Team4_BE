package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.user.dto.request.UpdateProfileRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateStylesRequest;
import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.entity.SocialAccount;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.domain.user.repository.SocialAccountRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.global.auth.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStyleRepository userStyleRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final StyleRepository styleRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        return toMyProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getProfileBio(),
                user.getExternalLinkUrl()
        );
    }

    @Transactional
    public MyProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        // 선택 필드는 null 전달 시 기존 값 유지
        String profileImageUrl = request.profileImageUrl() != null
                ? request.profileImageUrl() : user.getProfileImageUrl();
        String profileBio = request.profileBio() != null
                ? request.profileBio() : user.getProfileBio();
        String externalLinkUrl = request.externalLinkUrl() != null
                ? request.externalLinkUrl() : user.getExternalLinkUrl();

        user.updateProfile(
                request.nickname(),
                profileImageUrl,
                profileBio,
                externalLinkUrl,
                request.gender(),
                request.birthDate(),
                request.regionName(),
                request.regionCode()
        );

        return toMyProfileResponse(user);
    }

    @Transactional
    public void updateStyles(Long userId, UpdateStylesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        List<String> requestedCodes = request.styleCodes();
        if (requestedCodes.stream().distinct().count() != requestedCodes.size()) {
            throw new IllegalArgumentException("중복된 스타일 코드가 포함되어 있습니다.");
        }

        List<Style> allStyles = styleRepository.findAllByOrderByCodeAsc();
        Map<String, Style> styleByCode = allStyles.stream()
                .collect(Collectors.toMap(Style::getCode, style -> style));
        boolean hasUnknownStyle = requestedCodes.stream()
                .anyMatch(code -> !styleByCode.containsKey(code));
        if (hasUnknownStyle) {
            throw new IllegalArgumentException("존재하지 않는 스타일 코드가 포함되어 있습니다.");
        }

        List<UserStyle> existingList = userStyleRepository.findAllByUserId(userId);
        Map<Long, UserStyle> existingByStyleId = existingList.stream()
                .collect(Collectors.toMap(userStyle -> userStyle.getStyle().getId(), userStyle -> userStyle));
        Map<Long, Integer> preferenceMap = new LinkedHashMap<>();
        for (int i = 0; i < requestedCodes.size(); i++) {
            Style style = styleByCode.get(requestedCodes.get(i));
            preferenceMap.put(style.getId(), i == 0 ? 7 : 3);
        }

        List<UserStyle> newStyles = new ArrayList<>();
        for (Style style : allStyles) {
            UserStyle userStyle = existingByStyleId.get(style.getId());
            if (userStyle == null) {
                userStyle = UserStyle.builder()
                        .user(user)
                        .style(style)
                        .build();
                newStyles.add(userStyle);
            }
            userStyle.updatePreferenceWeight(preferenceMap.getOrDefault(style.getId(), 0));
        }
        userStyleRepository.saveAll(newStyles);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        user.withdraw();
        refreshTokenService.delete(userId);
    }

    @Transactional
    public void restoreWithdrawnUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        if (user.getStatus() != UserStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴 상태의 사용자만 복구할 수 있습니다.");
        }
        if (user.getWithdrawnAt() == null
                || !user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30))) {
            throw new IllegalStateException("탈퇴 후 30일이 경과하여 계정을 복구할 수 없습니다.");
        }

        user.restore();
    }

    @Transactional(readOnly = true)
    public MarketingConsentResponse getMarketingConsent(Long userId) {
        User user = getUser(userId);
        return toMarketingConsentResponse(user);
    }

    @Transactional
    public MarketingConsentResponse updateMarketingConsent(Long userId, boolean marketingAgreed) {
        User user = getUser(userId);
        user.updateMarketingAgreement(marketingAgreed);
        return toMarketingConsentResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));
    }

    private MarketingConsentResponse toMarketingConsentResponse(User user) {
        return new MarketingConsentResponse(
                user.getMarketingAgreed()
        );
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        List<String> styleCodes = userStyleRepository.findAllByUserId(user.getId()).stream()
                .filter(userStyle -> userStyle.getPreferenceWeight() > 0)
                .sorted(
                        Comparator.comparing(UserStyle::getPreferenceWeight, Comparator.reverseOrder())
                                .thenComparing(userStyle -> userStyle.getStyle().getCode())
                )
                .map(userStyle -> userStyle.getStyle().getCode())
                .toList();

        List<String> socialProviders = socialAccountRepository.findAllByUserId(user.getId()).stream()
                .map(SocialAccount::getProvider)
                .distinct()
                .sorted()
                .toList();

        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                isOnboarded(user, styleCodes),
                user.getBirthDate(),
                user.getGender(),
                user.getRegionName(),
                user.getRegionCode(),
                user.getProfileImageUrl(),
                user.getProfileBio(),
                user.getExternalLinkUrl(),
                styleCodes,
                socialProviders
        );
    }

    private boolean isOnboarded(User user, List<String> styleCodes) {
        return user.getGender() != null
                && user.getGender() != User.Gender.OTHER
                && user.getBirthDate() != null
                && user.getRegionCode() != null
                && !user.getRegionCode().isBlank()
                && !styleCodes.isEmpty();
    }
}
