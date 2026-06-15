package com.closetnangam.be.domain.outfit.dto.request;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class OutfitCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    private String description;

    @Size(max = 500)
    // @URL(message = "올바른 URL 형식이 아닙니다.") // 필요하다면 @URL 추가 가능!
    private String thumbnailUrl;

    @NotBlank
    @Size(max = 50)
    private String situation;

    @NotBlank
    @Size(max = 50)
    private String season;

    private Boolean favorite = Boolean.FALSE;

    @Valid
    private List<OutfitItemRequest> items;

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
