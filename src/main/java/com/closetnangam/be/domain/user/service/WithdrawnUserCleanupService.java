package com.closetnangam.be.domain.user.service;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.domain.user.repository.SocialAccountRepository;
import com.closetnangam.be.domain.user.repository.UserExternalLinkRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private static final int WITHDRAWAL_RETENTION_DAYS = 30;
    private static final String WITHDRAWN_EMAIL_PREFIX = "withdrawn-";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserStyleRepository userStyleRepository;
    private final UserExternalLinkRepository userExternalLinkRepository;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupExpiredWithdrawnUsers() {
        cleanupExpiredWithdrawnUsers(LocalDateTime.now());
    }

    @Transactional
    int cleanupExpiredWithdrawnUsers(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(WITHDRAWAL_RETENTION_DAYS);
        List<User> expiredUsers = userRepository.findExpiredWithdrawnUsersForCleanup(
                UserStatus.WITHDRAWN,
                cutoff,
                WITHDRAWN_EMAIL_PREFIX
        );

        for (User user : expiredUsers) {
            Long userId = user.getId();
            socialAccountRepository.deleteAllByUserId(userId);
            userStyleRepository.deleteAllByUserId(userId);
            userExternalLinkRepository.deleteAllByUserId(userId);
            user.anonymizeAfterWithdrawalRetention();
        }

        return expiredUsers.size();
    }
}
