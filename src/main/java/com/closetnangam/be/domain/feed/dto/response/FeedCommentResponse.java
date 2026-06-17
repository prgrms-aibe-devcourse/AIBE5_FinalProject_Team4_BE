package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.feed.entity.FeedComment;

import java.time.LocalDateTime;
import java.util.List;

public record FeedCommentResponse(
        Long feedCommentId,
        Long feedPostId,
        FeedAuthorResponse author,
        Long parentCommentId,
        String content,
        List<FeedCommentResponse> replies,
        LocalDateTime createdAt,
        boolean isOwner
) {

    public static FeedCommentResponse from(FeedComment comment, List<FeedCommentResponse> replies, Long viewerUserId) {
        boolean isOwner = viewerUserId != null && viewerUserId.equals(comment.getAuthor().getId());
        return new FeedCommentResponse(
                comment.getId(),
                comment.getFeedPost().getId(),
                FeedAuthorResponse.from(comment.getAuthor()),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                replies == null ? List.of() : replies,
                comment.getCreatedAt(),
                isOwner
        );
    }
}
