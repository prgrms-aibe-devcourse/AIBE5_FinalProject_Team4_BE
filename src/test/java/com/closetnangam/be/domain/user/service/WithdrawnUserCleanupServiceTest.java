package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.domain.user.repository.SocialAccountRepository;
import com.closetnangam.be.domain.user.repository.UserExternalLinkRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawnUserCleanupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private UserStyleRepository userStyleRepository;

    @Mock
    private UserExternalLinkRepository userExternalLinkRepository;

    @InjectMocks
    private WithdrawnUserCleanupService cleanupService;

    @Test
    void cleanupExpiredWithdrawnUsers_anonymizesPersonalFieldsAndDeletesDirectLinks() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 3, 0);
        User user = User.builder()
                .email("user@example.com")
                .nickname("closetnangam")
                .profileImageUrl("https://example.com/profile.png")
                .profileBio("bio")
                .externalLinkUrl("https://example.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(User.Gender.MALE)
                .regionName("서울특별시")
                .regionCode("SEOUL")
                .marketingAgreed(true)
                .marketingAgreedAt(now.minusDays(40))
                .status(UserStatus.WITHDRAWN)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);
        ReflectionTestUtils.setField(user, "withdrawnAt", now.minusDays(31));

        when(userRepository.findExpiredWithdrawnUsersForCleanup(UserStatus.WITHDRAWN, now.minusDays(30), "withdrawn-"))
                .thenReturn(List.of(user));

        int cleanedCount = cleanupService.cleanupExpiredWithdrawnUsers(now);

        assertThat(cleanedCount).isEqualTo(1);
        assertThat(user.getEmail()).isEqualTo("withdrawn-10@deleted.closetnangam.local");
        assertThat(user.getNickname()).isNull();
        assertThat(user.getProfileImageUrl()).isEmpty();
        assertThat(user.getProfileBio()).isEmpty();
        assertThat(user.getExternalLinkUrl()).isEmpty();
        assertThat(user.getBirthDate()).isNull();
        assertThat(user.getGender()).isEqualTo(User.Gender.OTHER);
        assertThat(user.getRegionName()).isEmpty();
        assertThat(user.getRegionCode()).isEmpty();
        assertThat(user.getMarketingAgreed()).isFalse();
        assertThat(user.getMarketingAgreedAt()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawnAt()).isEqualTo(now.minusDays(31));

        verify(socialAccountRepository).deleteAllByUserId(10L);
        verify(userStyleRepository).deleteAllByUserId(10L);
        verify(userExternalLinkRepository).deleteAllByUserId(10L);
    }

    @Test
    void cleanupExpiredWithdrawnUsers_doesNotProcessAlreadyAnonymizedUsersAgain() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 3, 0);

        when(userRepository.findExpiredWithdrawnUsersForCleanup(UserStatus.WITHDRAWN, now.minusDays(30), "withdrawn-"))
                .thenReturn(List.of());

        int cleanedCount = cleanupService.cleanupExpiredWithdrawnUsers(now);

        assertThat(cleanedCount).isZero();
        verifyNoInteractions(socialAccountRepository, userStyleRepository, userExternalLinkRepository);
    }
}
