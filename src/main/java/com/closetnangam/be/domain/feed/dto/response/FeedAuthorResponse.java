package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.user.entity.User;

public record FeedAuthorResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {

    public static FeedAuthorResponse from(User user) {
        return new FeedAuthorResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
