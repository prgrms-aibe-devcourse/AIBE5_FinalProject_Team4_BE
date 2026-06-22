package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.user.entity.User;

public record FeedAuthorResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        Boolean followedByMe
) {

    public static FeedAuthorResponse from(User user) {
        return from(user, null);
    }

    public static FeedAuthorResponse from(User user, Boolean followedByMe) {
        return new FeedAuthorResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                followedByMe
        );
    }
}
