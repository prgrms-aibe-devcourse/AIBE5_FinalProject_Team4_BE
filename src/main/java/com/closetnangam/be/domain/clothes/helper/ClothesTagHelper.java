package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClothesTagHelper {

    private final StyleRepository styleRepository;
    private final CategoryCatalogService categoryCatalogService;

    public void validateClassification(
            String category,
            String itemType,
            String primaryColor,
            List<String> secondaryColors,
            List<String> styles
    ) {
        categoryCatalogService.validateCategoryAndItemType(category, itemType);
        categoryCatalogService.validateClothesColors(primaryColor, secondaryColors);
        categoryCatalogService.validateStyleCodes(styles);
    }

    public void applyColorTags(Clothes clothes, String primaryColor, List<String> secondaryColors) {
        buildColorTags(clothes, primaryColor, secondaryColors).forEach(clothes::addColorTag);
    }

    public void replaceColorTags(Clothes clothes, String primaryColor, List<String> secondaryColors) {
        clothes.replaceColorTags(buildColorTags(clothes, primaryColor, secondaryColors));
    }

    public void applyStyleTags(Clothes clothes, List<String> styleCodes) {
        buildStyleTags(clothes, styleCodes).forEach(clothes::addStyleTag);
    }

    public void replaceStyleTags(Clothes clothes, List<String> styleCodes) {
        clothes.replaceStyleTags(buildStyleTags(clothes, styleCodes));
    }

    private List<ClothingColor> buildColorTags(
            Clothes clothes,
            String primaryColor,
            List<String> secondaryColors
    ) {
        List<ClothingColor> colorTags = new ArrayList<>();
        colorTags.add(ClothingColor.create(clothes, primaryColor, ColorRole.PRIMARY, (byte) 0));

        if (secondaryColors != null && !secondaryColors.isEmpty()) {
            byte sortOrder = 1;
            for (String secondaryColor : secondaryColors) {
                colorTags.add(ClothingColor.create(clothes, secondaryColor, ColorRole.SECONDARY, sortOrder++));
            }
        }
        return colorTags;
    }

    private List<ClothesStyleTag> buildStyleTags(Clothes clothes, List<String> styleCodes) {
        List<Style> styles = styleRepository.findByCodeIn(styleCodes);
        if (styles.size() != styleCodes.size()) {
            throw new IllegalArgumentException("존재하지 않는 스타일 코드가 포함되어 있습니다.");
        }

        Map<String, Style> styleMap = styles.stream()
                .collect(Collectors.toMap(Style::getCode, Function.identity()));

        List<ClothesStyleTag> styleTags = new ArrayList<>();
        byte sortOrder = 0;
        for (String styleCode : styleCodes) {
            Style style = styleMap.get(styleCode);
            StyleRole styleRole = sortOrder == 0 ? StyleRole.PRIMARY : StyleRole.SECONDARY;
            styleTags.add(ClothesStyleTag.create(clothes, style, styleRole, sortOrder++));
        }
        return styleTags;
    }
}
