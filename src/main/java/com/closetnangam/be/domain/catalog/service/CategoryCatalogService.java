package com.closetnangam.be.domain.catalog.service;

import com.closetnangam.be.domain.catalog.constants.CatalogLimits;
import com.closetnangam.be.domain.catalog.dto.response.*;
import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.catalog.enums.ExternalSource;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                                "primaryColor",
                                "주 색상",
                                "색상 코드(code)를 clothing_colors 테이블 PRIMARY 역할로 저장합니다.",
                                "WHITE"
                        ),
                        new GuideFieldResponse(
                                "secondaryColors",
                                "보조 색상",
                                "색상 코드 배열입니다. clothing_colors 테이블 SECONDARY 역할로 저장합니다. "
                                        + "최대 " + CatalogLimits.MAX_SECONDARY_COLORS + "개(primaryColor 제외).",
                                "NAVY"
                        ),
                        new GuideFieldResponse(
                                "styles",
                                "스타일",
                                "스타일 코드 배열입니다. styles 목록의 code 값을 사용합니다. "
                                        + "최대 " + CatalogLimits.MAX_STYLES + "개.",
                                "CASUAL"
                        )
                ),
                List.of(
                        "API 응답의 code 값을 서버 저장값으로 사용하세요. name(한글)은 화면 라벨용입니다.",
                        "colors.hex는 UI 표시용입니다. DB에는 colors.code만 저장합니다.",
                        "WHITE 색상 원은 밝은 배경에서 border가 필요할 수 있습니다.",
                        "item_type은 반드시 선택한 category 하위 코드만 사용할 수 있습니다.",
                        "AI 분류 프롬프트가 필요하면 GET /api/v1/categories/ai-guide 를 사용하세요."
                ),
                Map.of(
                        "clothesRegistration",
                        Map.of(
                                "category", "TOP",
                                "item_type", "SHORT_SLEEVE",
                                "primaryColor", "WHITE",
                                "secondaryColors", List.of("NAVY"),
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

        guide.append("\n[itemType]\n");
        for (ClothesCategory category : ClothesCategory.values()) {
            guide.append(category.name()).append(":\n");
            for (ClothesItemType itemType : ClothesItemType.byCategory(category)) {
                guide.append("  - ").append(itemType.name())
                        .append(" (").append(itemType.getLabel()).append(")\n");
            }
        }

        guide.append("\n[primaryColor]\n");
        guide.append("옷의 대표 색상 코드 1개. 아래 color 코드 목록에서 선택하세요.\n");
        guide.append(Arrays.stream(ClothesColor.values())
                .map(color -> color.name() + " (" + color.getLabel() + ")")
                .collect(Collectors.joining(", ")));

        guide.append("\n\n[secondaryColors]\n");
        guide.append("보조 색상 코드 배열. 없으면 빈 배열 []을 사용하고, primaryColor와 중복되면 안 됩니다. ");
        guide.append("최대 ").append(CatalogLimits.MAX_SECONDARY_COLORS).append("개.\n");

        guide.append("\n\n[styles]\n");
        guide.append("스타일 코드 배열. 최소 1개, 최대 ").append(CatalogLimits.MAX_STYLES).append("개.\n");
        guide.append(Arrays.stream(StyleCode.values())
                .map(style -> style.name() + " (" + style.getLabel() + ")")
                .collect(Collectors.joining(", ")));

        guide.append("\n\n응답 JSON 예시:\n");
        guide.append("""
                {
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": ["NAVY"],
                  "styles": ["CASUAL", "MINIMAL"]
                }
                """);

        return guide.toString();
    }

    public String getPurchaseCaptureExtractionGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append(getAiClassificationGuide());
        guide.append("\n\n[externalSource]\n");
        guide.append("쇼핑몰 코드(suggestedExternalSource). 아래 코드만 사용하고, 확실하지 않으면 null.\n");
        guide.append(Arrays.stream(ExternalSource.values())
                .filter(source -> !source.isAllowsCustomInput())
                .map(source -> source.name() + " (" + source.getLabel() + ")")
                .collect(Collectors.joining(", ")));
        guide.append("\n직접입력 쇼핑몰명은 suggestedExternalSource에 넣지 말고 null로 두세요.");
        return guide.toString();
    }

    public void validateClothesClassification(String categoryCode, String itemTypeCode, String colorCode) {
        validateCategoryAndItemType(categoryCode, itemTypeCode);
        validateColorCode(colorCode);
    }

    public void validateCategoryAndItemType(String categoryCode, String itemTypeCode) {
        ClothesCategory.fromCode(categoryCode);
        if (!ClothesItemType.matchesCategory(categoryCode, itemTypeCode)) {
            throw new IllegalArgumentException("item_type이 category와 일치하지 않습니다.");
        }
    }

    public void validateColorCode(String colorCode) {
        ClothesColor.fromCode(colorCode);
    }

    public void validateClothesColors(String primaryColor, List<String> secondaryColors) {
        validateColorCode(primaryColor);
        if (secondaryColors == null || secondaryColors.isEmpty()) {
            return;
        }
        if (secondaryColors.size() > CatalogLimits.MAX_SECONDARY_COLORS) {
            throw new IllegalArgumentException(
                    "보조 색상은 최대 " + CatalogLimits.MAX_SECONDARY_COLORS + "개까지 선택할 수 있습니다.");
        }
        Set<String> seen = new HashSet<>();
        for (String secondaryColor : secondaryColors) {
            validateColorCode(secondaryColor);
            if (primaryColor.equals(secondaryColor)) {
                throw new IllegalArgumentException("주 색상과 보조 색상은 같을 수 없습니다.");
            }
            if (!seen.add(secondaryColor)) {
                throw new IllegalArgumentException("보조 색상에 중복된 값이 있습니다: " + secondaryColor);
            }
        }
    }

    public void validateStyleCodes(List<String> styleCodes) {
        if (styleCodes == null || styleCodes.isEmpty()) {
            throw new IllegalArgumentException("스타일은 1개 이상 선택해야 합니다.");
        }
        if (styleCodes.size() > CatalogLimits.MAX_STYLES) {
            throw new IllegalArgumentException(
                    "스타일은 최대 " + CatalogLimits.MAX_STYLES + "개까지 선택할 수 있습니다.");
        }
        Set<String> seen = new HashSet<>();
        for (String styleCode : styleCodes) {
            StyleCode.fromCode(styleCode);
            if (!seen.add(styleCode)) {
                throw new IllegalArgumentException("스타일에 중복된 값이 있습니다: " + styleCode);
            }
        }
    }

    public ExternalSourcesResponse getExternalSources() {
        return new ExternalSourcesResponse(
                """
                        미보유 옷 저장 시 사용할 외부 쇼핑 출처 목록입니다.
                        - 목록에서 선택: externalSource에 code 값(예: MUSINSA)을 저장합니다.
                        - 직접입력: 사용자가 입력한 출처명을 externalSource VARCHAR에 그대로 저장합니다.
                        DB 컬럼은 enum이 아닌 VARCHAR(50)입니다.
                        """,
                Arrays.stream(ExternalSource.values())
                        .map(ExternalSourceResponse::from)
                        .toList()
        );
    }

    public void validateExternalSource(String externalSource) {
        if (!StringUtils.hasText(externalSource)) {
            throw new IllegalArgumentException("외부 출처는 필수입니다.");
        }
        if (externalSource.length() > 50) {
            throw new IllegalArgumentException("외부 출처는 50자 이하여야 합니다.");
        }
        if ("NONE".equals(externalSource)) {
            throw new IllegalArgumentException("외부 출처를 선택하거나 입력해 주세요.");
        }

        ExternalSource.findByCode(externalSource).ifPresent(matched -> {
            if (matched.isAllowsCustomInput()) {
                throw new IllegalArgumentException("직접입력 출처명을 입력해 주세요.");
            }
        });
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
