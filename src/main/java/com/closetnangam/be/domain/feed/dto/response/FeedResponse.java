package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.feed.entity.FeedPost;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;

import java.time.LocalDateTime;
import java.util.List;

public record FeedResponse(
        Long feedPostId,
        FeedAuthorResponse author,
        OutfitResponse outfit,
        String caption,
        List<FeedImageResponse> images,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        boolean savedByMe,
        boolean hidden,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FeedResponse of(
            FeedPost post,
            OutfitResponse outfit,
            long likeCount,
            long commentCount,
            boolean likedByMe,
            boolean savedByMe,
            Boolean authorFollowedByMe,
            Long viewerUserId
    ) {
        return new FeedResponse(
                post.getId(),
                FeedAuthorResponse.from(post.getAuthor(), authorFollowedByMe),
                outfit,
                post.getCaption(),
                post.getImages().stream().map(FeedImageResponse::from).toList(),
                likeCount,
                commentCount,
                likedByMe,
                savedByMe,
                post.isHidden(),
                viewerUserId != null && viewerUserId.equals(post.getAuthor().getId()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
