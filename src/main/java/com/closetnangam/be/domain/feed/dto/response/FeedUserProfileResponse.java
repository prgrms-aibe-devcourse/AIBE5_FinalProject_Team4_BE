package com.closetnangam.be.domain.feed.dto.response;

import com.closetnangam.be.domain.user.entity.User;

public record FeedUserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String profileBio,
        String externalLinkUrl,
        long postCount,
        long followerCount,
        long followingCount,
        boolean followedByMe,
        boolean mine
) {

    public static FeedUserProfileResponse of(
            User user,
            long postCount,
            long followerCount,
            long followingCount,
            boolean followedByMe,
            boolean mine
    ) {
        return new FeedUserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getProfileBio(),
                user.getExternalLinkUrl(),
                postCount,
                followerCount,
                followingCount,
                followedByMe,
                mine
        );
    }
}
