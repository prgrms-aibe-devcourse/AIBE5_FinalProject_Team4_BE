package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.entity.OutfitStyles;
import com.closetnangam.be.domain.outfit.repository.OutfitStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutfitStyleService {

    private final OutfitStyleRepository outfitStyleRepository;

    public void saveOutfitStyles(Outfit outfit, List<OutfitItem> items) {
        Map<Long, OutfitStyles> styleMap = new LinkedHashMap<>();

        for (OutfitItem item : items) {
            item.getClothes().getSortedStyleTags().forEach(tag -> {
                Long styleId = tag.getStyle().getId();
                styleMap.merge(styleId,
                        OutfitStyles.create(outfit, tag.getStyle(), tag.getStyleRole(), tag.getSortOrder()),
                        (existing, incoming) -> {
                            if (existing.getStyleRole() == StyleRole.PRIMARY) return existing;
                            return incoming;
                        }
                );
            });
        }

        outfitStyleRepository.saveAll(styleMap.values());
    }

    public void deleteOutfitStyles(Long outfitId) {
        outfitStyleRepository.deleteAllByOutfit_OutfitId(outfitId);
    }
}