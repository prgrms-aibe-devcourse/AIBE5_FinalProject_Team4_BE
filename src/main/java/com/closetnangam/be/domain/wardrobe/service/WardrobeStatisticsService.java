package com.closetnangam.be.domain.wardrobe.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeStatisticsResponse;
import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeStatisticsResponse.ItemTypeCount;
import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeStatisticsResponse.UserStyleWardrobePayload;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardrobeStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(WardrobeStatisticsService.class);
    private static final double PRIMARY_STYLE_WEIGHT = 0.7;
    private static final double SECONDARY_STYLE_WEIGHT = 0.3;

    private final WardrobeRepository wardrobeRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final UserStyleRepository userStyleRepository;
    private final UserRepository userRepository;
    private final StyleRepository styleRepository;

    @Transactional
    public WardrobeStatisticsResponse getStatistics(Long userId) {
        Wardrobe wardrobe = wardrobeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("옷장을 찾을 수 없습니다."));

        ComputedStatistics computed = computeStatistics(userId);
        syncUserStyles(userId, computed.stylePayloads(), computed.hasWardrobeData());
        markStatisticsSynced(userId);

        return new WardrobeStatisticsResponse(
                userId,
                wardrobe.getId(),
                computed.ownedCount(),
                toItemTypeCounts(computed.itemTypeCounts()),
                computed.stylePayloads()
        );
    }

    /**
     * 보유 옷 등록·수정·삭제 후 {@code user_styles.wardrobe_weight}를 동기화합니다.
     * 추천 API 호출마다 실행하지 않고 옷장 데이터가 바뀔 때만 호출합니다.
     */
    @Transactional
    public void syncAfterWardrobeChange(Long userId) {
        if (wardrobeRepository.findByUser_Id(userId).isEmpty()) {
            return;
        }
        ComputedStatistics computed = computeStatistics(userId);
        syncUserStyles(userId, computed.stylePayloads(), computed.hasWardrobeData());
        markStatisticsSynced(userId);
    }

    /**
     * 추천 API 진입 전 옷장 통계가 최신인지 확인하고, stale이면 1회 동기화합니다.
     *
     * <p>배포 직후 기존 사용자(옷장 보유 + {@code statistics_synced_at} 미기록)나
     * 동기화 누락 구간을 커버합니다. 이미 최신이면 읽기만 하고 DB write는 하지 않습니다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSyncedForRecommendation(Long userId) {
        Optional<Wardrobe> wardrobe = wardrobeRepository.findByUser_Id(userId);
        if (wardrobe.isEmpty()) {
            return;
        }
        if (!wardrobeClothesRepository.existsByUserIdAndOwnershipStatus(userId, OwnershipStatus.OWNED)) {
            return;
        }
        if (!needsResync(userId, wardrobe.get())) {
            return;
        }
        log.info("[옷장통계] 추천 진입 전 동기화. userId={}", userId);
        ComputedStatistics computed = computeStatistics(userId);
        syncUserStyles(userId, computed.stylePayloads(), computed.hasWardrobeData());
        markStatisticsSynced(userId);
    }

    private boolean needsResync(Long userId, Wardrobe wardrobe) {
        if (wardrobe.getStatisticsSyncedAt() == null) {
            return true;
        }
        Optional<LocalDateTime> latestChange = wardrobeClothesRepository.findLatestUpdateAtByUserIdAndOwnershipStatus(
                userId,
                OwnershipStatus.OWNED
        );
        return latestChange.isPresent() && latestChange.get().isAfter(wardrobe.getStatisticsSyncedAt());
    }

    private void markStatisticsSynced(Long userId) {
        wardrobeRepository.findByUser_Id(userId).ifPresent(wardrobe -> {
            wardrobe.markStatisticsSynced(LocalDateTime.now());
        });
    }

    private ComputedStatistics computeStatistics(Long userId) {
        List<WardrobeClothes> ownedClothes = wardrobeClothesRepository.findOwnedForStatistics(
                userId, OwnershipStatus.OWNED
        );

        Map<String, Integer> itemTypeCounts = new HashMap<>();
        Map<Long, StyleAccumulator> styleAccumulators = new LinkedHashMap<>();

        for (WardrobeClothes wardrobeClothes : ownedClothes) {
            Clothes clothes = wardrobeClothes.getClothes();
            if (clothes == null || !StringUtils.hasText(clothes.getItemType())) {
                continue;
            }

            itemTypeCounts.merge(clothes.getItemType(), 1, Integer::sum);

            for (ClothesStyleTag styleTag : clothes.getSortedStyleTags()) {
                if (styleTag == null || styleTag.getStyle() == null) {
                    continue;
                }
                Style style = styleTag.getStyle();
                double tagWeight = StyleRole.PRIMARY.equals(styleTag.getStyleRole())
                        ? PRIMARY_STYLE_WEIGHT
                        : SECONDARY_STYLE_WEIGHT;

                styleAccumulators
                        .computeIfAbsent(style.getId(), id -> new StyleAccumulator(style))
                        .add(tagWeight);
            }
        }

        List<UserStyleWardrobePayload> stylePayloads = toUserStylePayloads(styleAccumulators);
        return new ComputedStatistics(
                ownedClothes.size(),
                itemTypeCounts,
                stylePayloads,
                !ownedClothes.isEmpty()
        );
    }

    private void syncUserStyles(Long userId, List<UserStyleWardrobePayload> payloads, boolean hasWardrobeData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<UserStyle> existingStyles = userStyleRepository.findAllByUserId(userId);
        Map<Long, UserStyle> existingStyleMap = new HashMap<>();
        for (UserStyle us : existingStyles) {
            existingStyleMap.put(us.getStyle().getId(), us);
        }

        boolean dirty = false;

        // 1. 현재 옷장 통계(payloads)에 있는 스타일들 처리 (기존 데이터 업데이트 또는 신규 생성)
        for (UserStyleWardrobePayload payload : payloads) {
            UserStyle userStyle = existingStyleMap.get(payload.styleId());
            if (userStyle == null) {
                Style style = styleRepository.findById(payload.styleId())
                        .orElseThrow(() -> new IllegalArgumentException("스타일을 찾을 수 없습니다. styleId=" + payload.styleId()));
                userStyle = UserStyle.builder()
                        .user(user)
                        .style(style)
                        .build();
                userStyle.syncWardrobeWeight(payload.wardrobeWeight(), hasWardrobeData);
                userStyleRepository.save(userStyle);
                dirty = true;
            } else if (userStyle.syncWardrobeWeightIfChanged(payload.wardrobeWeight(), hasWardrobeData)) {
                userStyleRepository.save(userStyle);
                dirty = true;
            }
            existingStyleMap.remove(payload.styleId());
        }

        // 2. 현재 옷장에는 없지만 DB에는 남아있는 스타일들 처리 (wardrobeWeight = 0으로 초기화)
        for (UserStyle remainingStyle : existingStyleMap.values()) {
            if (remainingStyle.syncWardrobeWeightIfChanged(0, hasWardrobeData)) {
                userStyleRepository.save(remainingStyle);
                dirty = true;
            }
        }

        if (dirty) {
            userStyleRepository.flush();
        }
    }

    private record ComputedStatistics(
            int ownedCount,
            Map<String, Integer> itemTypeCounts,
            List<UserStyleWardrobePayload> stylePayloads,
            boolean hasWardrobeData
    ) {
    }

    private List<ItemTypeCount> toItemTypeCounts(Map<String, Integer> itemTypeCounts) {
        return itemTypeCounts.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> {
                    String itemType = entry.getKey();
                    ItemTypeMetadata metadata = resolveItemTypeMetadata(itemType);
                    return new ItemTypeCount(
                            itemType,
                            metadata.label(),
                            metadata.category(),
                            entry.getValue()
                    );
                })
                .toList();
    }

    private List<UserStyleWardrobePayload> toUserStylePayloads(Map<Long, StyleAccumulator> accumulators) {
        if (accumulators.isEmpty()) {
            return List.of();
        }

        double totalWeightedScore = accumulators.values().stream()
                .mapToDouble(StyleAccumulator::weightedScore)
                .sum();

        List<UserStyleWardrobePayload> payloads = new ArrayList<>();
        for (StyleAccumulator accumulator : accumulators.values()) {
            int wardrobeWeight = totalWeightedScore > 0.0
                    ? (int) Math.round(accumulator.weightedScore() / totalWeightedScore * 100.0)
                    : 0;

            Style style = accumulator.style();
            payloads.add(new UserStyleWardrobePayload(
                    style.getId(),
                    style.getCode(),
                    style.getName(),
                    accumulator.weightedScore(),
                    wardrobeWeight
            ));
        }

        payloads.sort(Comparator
                .comparingInt(UserStyleWardrobePayload::wardrobeWeight).reversed()
                .thenComparing(UserStyleWardrobePayload::styleCode));

        return payloads;
    }

    private ItemTypeMetadata resolveItemTypeMetadata(String itemTypeCode) {
        try {
            ClothesItemType itemType = ClothesItemType.fromCode(itemTypeCode);
            return new ItemTypeMetadata(itemType.getLabel(), itemType.getCategory().name());
        } catch (IllegalArgumentException exception) {
            return new ItemTypeMetadata(itemTypeCode, null);
        }
    }

    private record ItemTypeMetadata(String label, String category) {
    }

    private static final class StyleAccumulator {
        private final Style style;
        private double weightedScore;

        private StyleAccumulator(Style style) {
            this.style = style;
        }

        private void add(double weight) {
            this.weightedScore += weight;
        }

        private Style style() {
            return style;
        }

        private double weightedScore() {
            return weightedScore;
        }
    }
}
