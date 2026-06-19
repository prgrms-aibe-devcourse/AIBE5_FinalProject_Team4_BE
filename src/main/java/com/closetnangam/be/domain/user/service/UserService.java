package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.user.dto.request.CompleteOnboardingRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateProfileRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateStylesRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateGuideTourRequest;
import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.NicknameAvailabilityResponse;
import com.closetnangam.be.domain.user.dto.response.ProfileImageUploadResponse;
import com.closetnangam.be.domain.user.dto.response.SocialAccountResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.entity.SocialAccount;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.domain.user.repository.SocialAccountRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.user.support.NicknamePolicy;
import com.closetnangam.be.global.auth.jwt.RefreshTokenService;
import com.closetnangam.be.global.storage.ImageStorageService;
import com.closetnangam.be.global.storage.StoredImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.time.LocalDate;
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
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final RefreshTokenService refreshTokenService;
    private final ImageStorageService localImageStorageService;


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

    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse checkNicknameAvailability(Long userId, String nickname) {
        String normalizedNickname = NicknamePolicy.normalize(nickname);
        if (!NicknamePolicy.isValid(normalizedNickname)) {
            return new NicknameAvailabilityResponse(
                    normalizedNickname,
                    false,
                    NicknamePolicy.RULE_MESSAGE
            );
        }

        boolean duplicated = userRepository.existsByNicknameAndIdNot(normalizedNickname, userId);
        if (duplicated) {
            return new NicknameAvailabilityResponse(
                    normalizedNickname,
                    false,
                    "이미 사용 중인 닉네임입니다."
            );
        }
        return new NicknameAvailabilityResponse(
                normalizedNickname,
                true,
                "사용 가능한 닉네임입니다."
        );
    }

    @Transactional
    public MyProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        updateUserProfile(
                user,
                request.nickname(),
                request.birthDate(),
                request.gender(),
                request.regionName(),
                request.regionCode(),
                request.profileImageUrl(),
                request.profileBio(),
                request.externalLinkUrl()
        );

        return toMyProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile file) {
        getUser(userId);
        StoredImage storedImage = localImageStorageService.storeProfileImage(userId, file);
        return new ProfileImageUploadResponse(storedImage.publicUrl());
    }

    @Transactional
    public void updateStyles(Long userId, UpdateStylesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        updateUserStyles(user, request.styleCodes());
    }

    @Transactional
    public MyProfileResponse completeOnboarding(Long userId, CompleteOnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));

        updateUserProfile(
                user,
                request.nickname(),
                request.birthDate(),
                request.gender(),
                request.regionName(),
                request.regionCode(),
                null,
                null,
                null
        );
        updateUserStyles(user, request.styleCodes());
        user.updateMarketingAgreement(request.marketingAgreed());

        return toMyProfileResponse(user);
    }

    private void updateUserProfile(
            User user,
            String nickname,
            LocalDate birthDate,
            User.Gender gender,
            String regionName,
            String regionCode,
            String profileImageUrl,
            String profileBio,
            String externalLinkUrl
    ) {
        if (gender == User.Gender.OTHER) {
            throw new IllegalArgumentException("성별은 MALE 또는 FEMALE만 선택할 수 있습니다.");
        }
        String normalizedNickname = NicknamePolicy.normalize(nickname);
        NicknamePolicy.validate(normalizedNickname);
        if (userRepository.existsByNicknameAndIdNot(normalizedNickname, user.getId())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        // 선택 필드는 null 전달 시 기존 값 유지
        String nextProfileImageUrl = profileImageUrl != null
                ? profileImageUrl : user.getProfileImageUrl();
        String nextProfileBio = profileBio != null
                ? profileBio : user.getProfileBio();
        String nextExternalLinkUrl = externalLinkUrl != null
                ? externalLinkUrl : user.getExternalLinkUrl();

        user.updateProfile(
                normalizedNickname,
                nextProfileImageUrl,
                nextProfileBio,
                nextExternalLinkUrl,
                gender,
                birthDate,
                regionName,
                regionCode
        );
    }

    private void updateUserStyles(User user, List<String> requestedCodes) {
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

        List<UserStyle> existingList = userStyleRepository.findAllByUserId(user.getId());
        Map<Long, UserStyle> existingByStyleId = existingList.stream()
                .collect(Collectors.toMap(userStyle -> userStyle.getStyle().getId(), userStyle -> userStyle));
        Map<Long, Integer> preferenceMap = new LinkedHashMap<>();
        for (int i = 0; i < requestedCodes.size(); i++) {
            Style style = styleByCode.get(requestedCodes.get(i));
            preferenceMap.put(style.getId(), i == 0 ? 7 : 3);
        }

        boolean hasWardrobeData = wardrobeClothesRepository.existsByUserIdAndOwnershipStatus(user.getId(), OwnershipStatus.OWNED);

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
            userStyle.updatePreferenceWeight(preferenceMap.getOrDefault(style.getId(), 0), hasWardrobeData);
        }
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

        List<SocialAccount> socialAccountList = socialAccountRepository.findAllByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(SocialAccount::getProvider))
                .toList();

        List<String> socialProviders = socialAccountList.stream()
                .map(SocialAccount::getProvider)
                .distinct()
                .sorted()
                .toList();

        List<SocialAccountResponse> socialAccounts = socialAccountList.stream()
                .map(account -> new SocialAccountResponse(
                        account.getProvider(),
                        account.getProviderEmail()
                ))
                .toList();

        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                isOnboarded(user, styleCodes),
                user.isGuideTourCompletedHome(),
                user.isGuideTourCompletedWardrobe(),
                user.isGuideTourCompletedFeed(),
                user.isGuideTourCompletedMypage(),
                user.getGender(),
                user.getBirthDate(),
                user.getRegionName(),
                user.getRegionCode(),
                user.getProfileImageUrl(),
                user.getProfileBio(),
                user.getExternalLinkUrl(),
                styleCodes,
                socialProviders,
                socialAccounts
        );
    }

    private boolean isOnboarded(User user, List<String> styleCodes) {
        return user.getNickname() != null
                && !user.getNickname().isBlank()
                && user.getGender() != null
                && user.getGender() != User.Gender.OTHER
                && user.getBirthDate() != null
                && user.getRegionCode() != null
                && !user.getRegionCode().isBlank()
                && styleCodes.size() >= 2;
    }
}
