package com.closetnangam.be.domain.outfit.dto.request;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OutfitCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String thumbnailUrl;

    @NotBlank
    private String situation;

    @NotBlank
    private String season;

    private Boolean favorite = Boolean.FALSE;

    public Outfit toEntity(OutfitBook outfitBook) {
        return Outfit.builder()
                .outfitBook(outfitBook)
                .title(this.title)
                .description(this.description)
                .thumbnailUrl(this.thumbnailUrl)
                .situation(this.situation)
                .season(this.season)
                .favorite(Boolean.TRUE.equals(this.favorite))
                .build();
    }
}
