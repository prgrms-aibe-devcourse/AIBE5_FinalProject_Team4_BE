package com.closetnangam.be.domain.clothes.scoring;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 추천 점수 계산에 필요한 옷 태그 정보 스냅샷.
 * {@link Clothes#getRecommendationTagSnapshot()}에서 transient 캐시로 재사용됩니다.
 */
public record ClothesTagSnapshot(
        List<WeightedColor> weightedColors,
        List<String> styleCodes,
        String primaryColor,
        List<String> secondaryColorCodes,
        String primaryStyleCode
) {

    public record WeightedColor(String code, double weight) {
        /** 빈 색상 코드로 WeightedColor 를 생성하면 즉시 실패합니다 (fail-fast). */
        public WeightedColor {
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("WeightedColor code must not be blank");
            }
        }
    }

    public static ClothesTagSnapshot from(Clothes clothes) {
        List<WeightedColor> weightedColors = new ArrayList<>();
        List<String> secondaryColorCodes = new ArrayList<>();
        String primaryColor = null;

        // getSortedColorTags()는 Clothes.colorTags 가 new ArrayList<>() 로 초기화되어 null 을 반환하지 않습니다.
        for (ClothingColor colorTag : clothes.getSortedColorTags()) {
            if (!StringUtils.hasText(colorTag.getColorCode())) {
                continue;
            }
            ColorRole colorRole = colorTag.getColorRole();
            double weight = (colorRole != null) ? colorRole.getWeight() : ColorRole.SECONDARY.getWeight();
            weightedColors.add(new WeightedColor(
                    colorTag.getColorCode(),
                    weight
            ));
            if (ColorRole.PRIMARY.equals(colorRole) && primaryColor == null) {
                primaryColor = colorTag.getColorCode();
            } else if (ColorRole.SECONDARY.equals(colorRole)) {
                secondaryColorCodes.add(colorTag.getColorCode());
            }
        }

        List<String> styleCodes = new ArrayList<>();
        String primaryStyleCode = null;
        // getSortedStyleTags()는 Clothes.styleTags 가 new ArrayList<>() 로 초기화되어 null 을 반환하지 않습니다.
        for (ClothesStyleTag styleTag : clothes.getSortedStyleTags()) {
            if (styleTag.getStyle() == null) {
                continue;
            }
            String styleCode = styleTag.getStyle().getCode();
            if (!StringUtils.hasText(styleCode)) {
                continue;
            }
            styleCodes.add(styleCode);
            if (primaryStyleCode == null && isPrimaryStyle(styleTag)) {
                primaryStyleCode = styleCode;
            }
        }

        // PRIMARY(1.0) → SECONDARY(0.6) 순으로 정렬하여 computeColorScore() early-exit 효율 극대화
        weightedColors.sort(Comparator.comparingDouble(WeightedColor::weight).reversed());

        return new ClothesTagSnapshot(
                weightedColors,
                styleCodes,
                primaryColor,
                secondaryColorCodes,
                primaryStyleCode
        );
    }

    private static boolean isPrimaryStyle(ClothesStyleTag styleTag) {
        return StyleRole.PRIMARY.equals(styleTag.getStyleRole());
    }
}
