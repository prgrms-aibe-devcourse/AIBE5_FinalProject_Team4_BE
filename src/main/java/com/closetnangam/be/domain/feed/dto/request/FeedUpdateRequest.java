package com.closetnangam.be.domain.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FeedUpdateRequest(
        Long outfitId,
        @Size(max = 2000) String caption,
        @Size(max = 10, message = "피드 이미지는 최대 10장까지 등록할 수 있습니다.")
        List<@NotBlank @Size(max = 500) String> imageUrls,
        Boolean hidden
) {
}
