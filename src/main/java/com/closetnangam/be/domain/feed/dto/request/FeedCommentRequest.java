package com.closetnangam.be.domain.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedCommentRequest(
        Long parentCommentId,
        @NotBlank
        @Size(max = 1000)
        String content
) {
}
