package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClothesTagHelper {

    private final StyleRepository styleRepository;
    private final CategoryCatalogService categoryCatalogService;

    @PersistenceContext
    private EntityManager entityManager;

    public void validateClassification(
            String category,
            String itemType,
            String primaryColor,
            List<String> secondaryColors,
            List<String> styles,
            String gender
    ) {
        categoryCatalogService.validateCategoryAndItemType(category, itemType);
        categoryCatalogService.validateClothesColors(primaryColor, secondaryColors);
        categoryCatalogService.validateStyleCodes(styles);
        categoryCatalogService.validateGenderCode(gender);
    }

    public void validateExternalSource(String externalSource) {
        categoryCatalogService.validateExternalSource(externalSource);
    }

    public void validateSeasonIfPresent(String seasonCode) {
        if (StringUtils.hasText(seasonCode)) {
            categoryCatalogService.validateSeasonCode(seasonCode);
        }
    }

    public void applyColorTags(Clothes clothes, String primaryColor, List<String> secondaryColors) {
        CategoryCatalogService.ResolvedClothesColors resolved =
                categoryCatalogService.resolveClothesColors(primaryColor, secondaryColors);
        buildColorTags(clothes, resolved.primaryColor(), resolved.secondaryColors())
                .forEach(clothes::addColorTag);
    }

    public void replaceColorTags(Clothes clothes, String primaryColor, List<String> secondaryColors) {
        CategoryCatalogService.ResolvedClothesColors resolved =
                categoryCatalogService.resolveClothesColors(primaryColor, secondaryColors);
        if (hasSameColorTags(clothes, resolved.primaryColor(), resolved.secondaryColors())) {
            return;
        }
        clothes.replaceColorTags(Collections.emptyList());
        entityManager.flush();
        buildColorTags(clothes, resolved.primaryColor(), resolved.secondaryColors())
                .forEach(clothes::addColorTag);
    }

    public void applyStyleTags(Clothes clothes, List<String> styleCodes) {
        buildStyleTags(clothes, styleCodes).forEach(clothes::addStyleTag);
    }

    public void copyTagsFrom(Clothes source, Clothes target) {
        source.getSortedColorTags().forEach(color -> target.addColorTag(
                ClothingColor.create(target, color.getColorCode(), color.getColorRole(), color.getSortOrder())
        ));
        source.getSortedStyleTags().forEach(styleTag -> target.addStyleTag(
                ClothesStyleTag.create(target, styleTag.getStyle(), styleTag.getStyleRole(), styleTag.getSortOrder())
        ));
    }

    public void replaceStyleTags(Clothes clothes, List<String> styleCodes) {
        if (hasSameStyleCodes(clothes, styleCodes)) {
            return;
        }
        clothes.replaceStyleTags(Collections.emptyList());
        entityManager.flush();
        applyStyleTags(clothes, styleCodes);
    }

    private boolean hasSameStyleCodes(Clothes clothes, List<String> styleCodes) {
        List<String> current = clothes.getSortedStyleTags().stream()
                .map(tag -> tag.getStyle().getCode())
                .toList();
        return current.equals(styleCodes);
    }

    private boolean hasSameColorTags(
            Clothes clothes,
            String primaryColor,
            List<String> secondaryColors
    ) {
        List<ClothingColor> sorted = clothes.getSortedColorTags();
        if (sorted.isEmpty()) {
            return false;
        }

        String currentPrimary = sorted.stream()
                .filter(color -> ColorRole.PRIMARY.equals(color.getColorRole()))
                .map(ClothingColor::getColorCode)
                .findFirst()
                .orElse(null);
        if (!primaryColor.equals(currentPrimary)) {
            return false;
        }

        List<String> currentSecondary = sorted.stream()
                .filter(color -> ColorRole.SECONDARY.equals(color.getColorRole()))
                .map(ClothingColor::getColorCode)
                .toList();
        return currentSecondary.equals(secondaryColors);
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
