package com.closetnangam.be.domain.outfit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class OutfitUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 500)
    private String thumbnailUrl;

    @NotBlank
    @Size(max = 50)
    private String situation;

    @NotBlank
    @Size(max = 50)
    private String season;

    private Boolean favorite = Boolean.FALSE;

    private List<OutfitItemRequest> items = new ArrayList<>();
}
