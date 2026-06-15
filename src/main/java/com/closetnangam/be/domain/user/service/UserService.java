package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        return new MyProfileResponse(userId);
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
