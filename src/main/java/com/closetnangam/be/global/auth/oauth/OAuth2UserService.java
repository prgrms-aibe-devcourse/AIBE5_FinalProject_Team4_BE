package com.closetnangam.be.global.auth.oauth;

import com.closetnangam.be.domain.user.entity.SocialAccount;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.domain.user.repository.SocialAccountRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String providerUserId = extractProviderUserId(provider, oAuth2User);
        String email = extractEmail(provider, oAuth2User);
        String rawName = extractName(provider, oAuth2User);

        SocialAccount account = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> createAccount(provider, providerUserId, email, rawName));

        boolean withdrawnRestoreRequired = requiresWithdrawnRestore(account.getUser());
        if (!withdrawnRestoreRequired) {
            account.recordLogin(email);
        }

        return new CustomOAuth2User(oAuth2User, account.getUser().getId(), withdrawnRestoreRequired);
    }

    private boolean requiresWithdrawnRestore(User user) {
        if (user.getStatus() != UserStatus.WITHDRAWN) {
            return false;
        }
        if (user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30))) {
            return true;
        }
        throw new OAuth2AuthenticationException(
                new OAuth2Error("user_withdrawn"),
                "탈퇴 후 30일이 경과하여 재로그인할 수 없습니다."
        );
    }

    private SocialAccount createAccount(String provider, String providerUserId, String email, String rawName) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, rawName));

        return socialAccountRepository.save(
                SocialAccount.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .providerEmail(email)
                        .build()
        );
    }

    private User createUser(String email, String rawName) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .nickname(buildUniqueNickname(rawName))
                        .build()
        );
    }

    private String buildUniqueNickname(String base) {
        String candidate = base.isBlank() ? "user" : base;
        while (userRepository.existsByNickname(candidate)) {
            candidate = base + "#" + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        }
        return candidate;
    }

    @SuppressWarnings("unchecked")
    private String extractProviderUserId(String provider, OAuth2User user) {
        return switch (provider) {
            case "kakao" -> String.valueOf(user.getAttributes().get("id"));
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) user.getAttributes().get("response");
                yield String.valueOf(response.get("id"));
            }
            default -> String.valueOf(user.getAttributes().get("sub")); // google
        };
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(String provider, OAuth2User user) {
        String email = switch (provider) {
            case "kakao" -> {
                Map<String, Object> account = (Map<String, Object>) user.getAttributes().get("kakao_account");
                yield account != null ? String.valueOf(account.get("email")) : null;
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) user.getAttributes().get("response");
                yield response != null ? String.valueOf(response.get("email")) : null;
            }
            default -> String.valueOf(user.getAttributes().get("email")); // google
        };
        if (!StringUtils.hasText(email) || "null".equals(email)) {
            return provider + "_" + extractProviderUserId(provider, user) + "@noreply.invalid";
        }
        return email;
    }

    @SuppressWarnings("unchecked")
    private String extractName(String provider, OAuth2User user) {
        return switch (provider) {
            case "kakao" -> {
                Map<String, Object> props = (Map<String, Object>) user.getAttributes().getOrDefault("properties", Map.of());
                Object nickname = props.get("nickname");
                yield nickname != null ? String.valueOf(nickname) : "user";
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) user.getAttributes().get("response");
                Object name = response.get("name");
                yield name != null ? String.valueOf(name) : "user";
            }
            default -> {
                Object name = user.getAttributes().get("name");
                yield name != null ? String.valueOf(name) : "user";
            }
        };
    }
}
