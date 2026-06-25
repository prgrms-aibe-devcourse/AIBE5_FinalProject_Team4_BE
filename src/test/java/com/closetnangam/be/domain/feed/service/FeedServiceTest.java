package com.closetnangam.be.domain.feed.service;

import com.closetnangam.be.domain.feed.dto.request.FeedCreateRequest;
import com.closetnangam.be.domain.feed.entity.FeedPost;
import com.closetnangam.be.domain.feed.repository.FeedPostLikeRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostRepository;
import com.closetnangam.be.domain.feed.repository.FeedCommentRepository;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.outfit.service.OutfitService;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private FeedPostRepository feedPostRepository;
    @Mock private FeedCommentRepository feedCommentRepository;
    @Mock private FeedPostLikeRepository feedPostLikeRepository;
    @Mock private com.closetnangam.be.domain.feed.repository.UserFollowRepository userFollowRepository;
    @Mock private UserRepository userRepository;
    @Mock private OutfitRepository outfitRepository;
    @Mock private OutfitItemRepository outfitItemRepository;
    @Mock private WardrobeClothesRepository wardrobeClothesRepository;
    @Mock private LocalImageStorageService localImageStorageService;
    @Mock private OutfitService outfitService;

    @InjectMocks
    private FeedService feedService;

    @Test
    @DisplayName("피드 업로드 시 코디와 이미지를 연결한다")
    void createPost() {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getNickname()).willReturn("tester");
        given(user.getProfileImageUrl()).willReturn("https://example.com/profile.jpg");

        OutfitBook outfitBook = org.mockito.Mockito.mock(OutfitBook.class);
        Outfit outfit = org.mockito.Mockito.mock(Outfit.class);
        given(outfit.getOutfitBook()).willReturn(outfitBook);
        given(outfit.getOutfitId()).willReturn(10L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(outfitRepository.findActiveByOutfitIdAndUserId(10L, 1L)).willReturn(Optional.of(outfit));
        given(feedPostRepository.save(any(FeedPost.class))).willAnswer(invocation -> {
            FeedPost post = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(post, "id", 100L);
            return post;
        });
        given(feedPostLikeRepository.countByFeedPost_Id(100L)).willReturn(0L);
        given(feedCommentRepository.countByFeedPost_IdAndDeletedAtIsNull(100L)).willReturn(0L);
        given(outfitItemRepository.findAllByOutfit_OutfitId(10L)).willReturn(List.of());
        given(outfitService.isFeedOutfitSavedByUser(outfit, 1L)).willReturn(true);

        var response = feedService.createPost(1L, new FeedCreateRequest(
                10L,
                "오늘 코디",
                List.of("http://localhost:8080/api/v1/images/feed/1/sample.jpg")
        ));

        assertThat(response.feedPostId()).isEqualTo(100L);
        assertThat(response.caption()).isEqualTo("오늘 코디");
        assertThat(response.images()).hasSize(1);
        verify(feedPostRepository).save(any(FeedPost.class));
    }

    @Test
    @DisplayName("룩피드 프로필 조회 시 게시물·팔로우 통계를 반환한다")
    void getUserFeedProfile() {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(2L);
        given(user.getNickname()).willReturn("lookfeed-user");
        given(user.getProfileImageUrl()).willReturn("https://example.com/profile.jpg");
        given(user.getProfileBio()).willReturn("데일리룩 공유");
        given(user.getExternalLinkUrl()).willReturn("https://example.com");

        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(feedPostRepository.countPublicByAuthorId(2L)).willReturn(5L);
        given(userFollowRepository.countByFollowee_Id(2L)).willReturn(10L);
        given(userFollowRepository.countByFollower_Id(2L)).willReturn(3L);
        given(userFollowRepository.existsByFollower_IdAndFollowee_Id(1L, 2L)).willReturn(true);

        var response = feedService.getUserFeedProfile(2L, 1L);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.nickname()).isEqualTo("lookfeed-user");
        assertThat(response.postCount()).isEqualTo(5L);
        assertThat(response.followerCount()).isEqualTo(10L);
        assertThat(response.followingCount()).isEqualTo(3L);
        assertThat(response.followedByMe()).isTrue();
        assertThat(response.mine()).isFalse();
    }
}
