package com.closetnangam.be.domain.wardrobe.config;

import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.wardrobe.service.WardrobeStatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 배포 직후 기존 보유 옷 사용자의 {@code user_styles.wardrobe_weight}를 백필합니다.
 *
 * <p>저장된 {@code user_styles.wardrobe_weight}와 옷장에서 계산한 기대값을 비교해
 * stale 사용자만 {@link WardrobeStatisticsService#ensureSyncedForRecommendation(Long)}로 처리합니다.</p>
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class WardrobeStatisticsBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WardrobeStatisticsBackfillRunner.class);

    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final WardrobeStatisticsService wardrobeStatisticsService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> userIds = wardrobeClothesRepository.findDistinctUserIdsByOwnershipStatus(OwnershipStatus.OWNED);
        if (userIds.isEmpty()) {
            return;
        }
        log.info("[옷장통계] 배포 백필 시작. 대상 사용자 수={}", userIds.size());
        int synced = 0;
        for (Long userId : userIds) {
            try {
                wardrobeStatisticsService.ensureSyncedForRecommendation(userId);
                synced++;
            } catch (RuntimeException exception) {
                log.warn("[옷장통계] 백필 실패. userId={}: {}", userId, exception.getMessage());
            }
        }
        log.info("[옷장통계] 배포 백필 완료. 처리 시도={}", synced);
    }
}
