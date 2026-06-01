package com.closetnangam.be.domain.outfit.dto.request;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; // 이걸 추가해줘
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OutfitCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    // description은 엔티티 제한이 따로 없으면 그대로 둬도 돼!
    private String description;

    @NotBlank
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
