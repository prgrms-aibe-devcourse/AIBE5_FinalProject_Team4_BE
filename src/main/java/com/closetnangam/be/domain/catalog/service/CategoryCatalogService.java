package com.closetnangam.be.domain.catalog.service;

import com.closetnangam.be.domain.catalog.dto.response.*;
import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryCatalogService {

    private final StyleRepository styleRepository;

    public CategoryCatalogResponse getCatalog() {
        return new CategoryCatalogResponse(
                getUsageGuide(),
                getCategoryGroups(),
                getStyles(),
                getColors()
        );
    }

    public CategoryUsageGuideResponse getUsageGuide() {
        return new CategoryUsageGuideResponse(
                "공용 카테고리 코드 사용 안내. DB 저장·AI 분류·화면 표시 시 아래 규칙을 따릅니다.",
                List.of(
                        new GuideFieldResponse(
                                "category",
                                "대분류",
                                "의류 종류 대분류 코드입니다. TOP, BOTTOM, OUTER, SHOES 중 하나를 사용합니다.",
                                "TOP"
                        ),
                        new GuideFieldResponse(
                                "item_type",
                                "소분류",
                                "category에 속하는 세부 유형 코드입니다. 선택한 category와 반드시 짝이 맞아야 합니다.",
                                "SHORT_SLEEVE"
                        ),
                        new GuideFieldResponse(
                                "color",
                                "컬러",
                                "색상 코드(code)를 DB에 저장하고, 화면에는 hex 값으로 색상 원(swatch)을 표시합니다.",
                                "NAVY"
                        ),
                        new GuideFieldResponse(
                                "styles",
                                "스타일",
                                "스타일 코드 배열입니다. styles 목록의 code 값을 사용합니다.",
                                "CASUAL"
                        )
                ),
                List.of(
                        "API 응답의 code 값을 서버 저장값으로 사용하세요. name(한글)은 화면 라벨용입니다.",
                        "colors.hex는 UI 표시용입니다. DB에는 colors.code만 저장합니다.",
                        "WHITE 색상 원은 밝은 배경에서 border가 필요할 수 있습니다.",
                        "item_type은 반드시 선택한 category 하위 코드만 사용할 수 있습니다.",
                        "AI 분류 프롬프트가 필요하면 GET /api/categories/ai-guide 를 사용하세요."
                ),
                Map.of(
                        "clothesRegistration",
                        Map.of(
                                "category", "TOP",
                                "item_type", "SHORT_SLEEVE",
                                "color", "WHITE",
                                "styles", List.of("CASUAL", "MINIMAL")
                        ),
                        "colorDisplay",
                        Map.of(
                                "code", "NAVY",
                                "name", "네이비",
                                "hex", "#1F3A5F"
                        )
                )
        );
    }

    public String getAiClassificationGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("옷 분류 시 아래 코드만 사용하세요.\n\n");

        guide.append("[category]\n");
        for (ClothesCategory category : ClothesCategory.values()) {
            guide.append("- ").append(category.name())
                    .append(" (").append(category.getLabel()).append(")\n");
        }

        guide.append("\n[item_type]\n");
        for (ClothesCategory category : ClothesCategory.values()) {
            guide.append(category.name()).append(":\n");
            for (ClothesItemType itemType : ClothesItemType.byCategory(category)) {
                guide.append("  - ").append(itemType.name())
                        .append(" (").append(itemType.getLabel()).append(")\n");
            }
        }

        guide.append("\n[color]\n");
        guide.append(Arrays.stream(ClothesColor.values())
                .map(color -> color.name() + " (" + color.getLabel() + ")")
                .collect(Collectors.joining(", ")));

        guide.append("\n\n[style]\n");
        guide.append(Arrays.stream(StyleCode.values())
                .map(style -> style.name() + " (" + style.getLabel() + ")")
                .collect(Collectors.joining(", ")));

        guide.append("\n\n응답 JSON 예시:\n");
        guide.append("""
                {
                  "category": "TOP",
                  "item_type": "SHORT_SLEEVE",
                  "color": "WHITE",
                  "styles": ["CASUAL", "MINIMAL"]
                }
                """);

        return guide.toString();
    }

    public void validateClothesClassification(String categoryCode, String itemTypeCode, String colorCode) {
        ClothesCategory.fromCode(categoryCode);
        if (!ClothesItemType.matchesCategory(categoryCode, itemTypeCode)) {
            throw new IllegalArgumentException("item_type이 category와 일치하지 않습니다.");
        }
        ClothesColor.fromCode(colorCode);
    }

    public void validateStyleCodes(List<String> styleCodes) {
        if (styleCodes == null || styleCodes.isEmpty()) {
            throw new IllegalArgumentException("스타일은 1개 이상 선택해야 합니다.");
        }

        for (String styleCode : styleCodes) {
            StyleCode.fromCode(styleCode);
            if (!styleRepository.existsByCode(styleCode)) {
                throw new IllegalArgumentException("존재하지 않는 스타일 코드입니다: " + styleCode);
            }
        }
    }

    private java.util.List<CategoryGroupResponse> getCategoryGroups() {
        return Arrays.stream(ClothesCategory.values())
                .map(category -> new CategoryGroupResponse(
                        category.name(),
                        category.getLabel(),
                        ClothesItemType.byCategory(category).stream()
                                .map(itemType -> new ItemTypeResponse(
                                        itemType.name(),
                                        itemType.getLabel(),
                                        itemType.getDescription()
                                ))
                                .toList()
                ))
                .toList();
    }

    private java.util.List<StyleResponse> getStyles() {
        return styleRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toStyleResponse)
                .toList();
    }

    private java.util.List<ColorResponse> getColors() {
        return Arrays.stream(ClothesColor.values())
                .map(color -> new ColorResponse(color.name(), color.getLabel(), color.getHex()))
                .toList();
    }

    private StyleResponse toStyleResponse(Style style) {
        return new StyleResponse(
                style.getId(),
                style.getCode(),
                style.getName(),
                style.getDescription()
        );
    }
}
