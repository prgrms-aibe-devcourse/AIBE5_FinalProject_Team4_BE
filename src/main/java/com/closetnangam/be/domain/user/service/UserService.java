package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.user.dto.request.UpdateGuideTourRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateProfileRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateStylesRequest;
import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.global.auth.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStyleRepository userStyleRepository;
    private final StyleRepository styleRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.isOnboarded(),
                user.isGuideTourCompletedHome(),
                user.isGuideTourCompletedWardrobe(),
                user.isGuideTourCompletedFeed(),
                user.isGuideTourCompletedMypage()
        );
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

        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.isOnboarded(),
                user.isGuideTourCompletedHome(),
                user.isGuideTourCompletedWardrobe(),
                user.isGuideTourCompletedFeed(),
                user.isGuideTourCompletedMypage()
        );
    }

    @Transactional
    public void updateStyles(Long userId, UpdateStylesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        List<Style> selectedStyles = styleRepository.findByCodeIn(request.styleCodes());
        if (selectedStyles.size() != request.styleCodes().size()) {
            throw new IllegalArgumentException("존재하지 않는 스타일 코드가 포함되어 있습니다.");
        }

        Map<String, Style> styleByCode = selectedStyles.stream()
                .collect(Collectors.toMap(Style::getCode, s -> s));
        List<UserStyle> existingList = userStyleRepository.findAllByUserId(userId);
        Map<Long, Integer> preferenceMap = new LinkedHashMap<>();
        List<String> requestedCodes = request.styleCodes();
        for (int i = 0; i < requestedCodes.size(); i++) {
            Style style = styleByCode.get(requestedCodes.get(i));
            preferenceMap.put(style.getId(), i == 0 ? 7 : 3);
        }

        for (UserStyle us : existingList) {
            int weight = preferenceMap.getOrDefault(us.getStyle().getId(), 0);
            us.updatePreferenceWeight(weight);
        }

        Set<Long> existingStyleIds = existingList.stream()
                .map(us -> us.getStyle().getId())
                .collect(Collectors.toSet());
        List<UserStyle> newStyles = selectedStyles.stream()
                .filter(style -> !existingStyleIds.contains(style.getId()))
                .map(style -> {
                    UserStyle us = UserStyle.builder().user(user).style(style).build();
                    int weight = preferenceMap.getOrDefault(style.getId(), 0);
                    us.updatePreferenceWeight(weight);
                    return us;
                })
                .toList();
        userStyleRepository.saveAll(newStyles);
    }

    @Transactional
    public void updateGuideTour(Long userId, UpdateGuideTourRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));
        user.updateGuideTour(request.home(), request.wardrobe(), request.feed(), request.mypage());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        user.withdraw();
        refreshTokenService.delete(userId);
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
}
