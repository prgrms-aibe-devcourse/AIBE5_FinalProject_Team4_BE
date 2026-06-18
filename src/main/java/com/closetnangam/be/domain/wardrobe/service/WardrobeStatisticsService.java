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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardrobeStatisticsService {

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
        syncUserStyles(userId, stylePayloads);

        return new WardrobeStatisticsResponse(
                userId,
                wardrobe.getId(),
                ownedClothes.size(),
                toItemTypeCounts(itemTypeCounts),
                stylePayloads
        );
    }

    private void syncUserStyles(Long userId, List<UserStyleWardrobePayload> payloads) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        for (UserStyleWardrobePayload payload : payloads) {
            Style style = styleRepository.findById(payload.styleId())
                    .orElseThrow(() -> new IllegalArgumentException("스타일을 찾을 수 없습니다. styleId=" + payload.styleId()));

            UserStyle userStyle = userStyleRepository.findByUserIdAndStyleId(userId, style.getId())
                    .orElseGet(() -> UserStyle.builder()
                            .user(user)
                            .style(style)
                            .build());

            userStyle.syncWardrobeWeight(payload.wardrobeWeight());
            userStyleRepository.save(userStyle);
        }
        userStyleRepository.flush(); // 즉시 반영하여 추천 서비스에서 최신 가중치를 읽을 수 있도록 함
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
