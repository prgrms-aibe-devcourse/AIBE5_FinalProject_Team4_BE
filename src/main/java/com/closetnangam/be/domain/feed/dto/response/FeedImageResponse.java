package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.feed.entity.FeedPostImage;

public record FeedImageResponse(
        Long feedPostImageId,
        String imageUrl,
        int sortOrder
) {

    public static FeedImageResponse from(FeedPostImage image) {
        return new FeedImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getSortOrder()
        );
    }
}
