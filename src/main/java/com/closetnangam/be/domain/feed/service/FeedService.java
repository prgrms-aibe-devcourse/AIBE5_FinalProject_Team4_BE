package com.closetnangam.be.domain.feed.service;

import com.closetnangam.be.domain.feed.dto.request.FeedCommentRequest;
import com.closetnangam.be.domain.feed.dto.request.FeedCreateRequest;
import com.closetnangam.be.domain.feed.dto.request.FeedUpdateRequest;
import com.closetnangam.be.domain.feed.dto.response.FeedCommentResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedImageUploadResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedInteractionResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedPageResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedResponse;
import com.closetnangam.be.domain.feed.entity.FeedComment;
import com.closetnangam.be.domain.feed.entity.FeedPost;
import com.closetnangam.be.domain.feed.entity.FeedPostImage;
import com.closetnangam.be.domain.feed.entity.FeedPostLike;
import com.closetnangam.be.domain.feed.entity.FeedPostSave;
import com.closetnangam.be.domain.feed.entity.UserFollow;
import com.closetnangam.be.domain.feed.repository.FeedCommentRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostLikeRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostRepository;
import com.closetnangam.be.domain.feed.repository.FeedPostSaveRepository;
import com.closetnangam.be.domain.feed.repository.UserFollowRepository;
import com.closetnangam.be.domain.outfit.dto.response.OutfitItemResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.outfit.service.OutfitService;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import com.closetnangam.be.global.storage.StoredImage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int MAX_PAGE_SIZE = 50;

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedPostLikeRepository feedPostLikeRepository;
    private final FeedPostSaveRepository feedPostSaveRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final LocalImageStorageService localImageStorageService;
    private final OutfitService outfitService;

    @Transactional
    public FeedResponse createPost(Long userId, FeedCreateRequest request) {
        User author = findUser(userId);
        Outfit outfit = resolveOwnedOutfit(userId, request.outfitId());

        FeedPost post = FeedPost.create(author, outfit, request.caption());
        attachImages(post, request.imageUrls());
        FeedPost saved = feedPostRepository.save(post);
        return toFeedResponse(saved, userId);
    }

    @Transactional
    public FeedResponse updatePost(Long postId, Long userId, FeedUpdateRequest request) {
        FeedPost post = findOwnedPost(postId, userId);

        if (request.caption() != null || request.outfitId() != null) {
            String caption = request.caption() != null ? request.caption() : post.getCaption();
            Outfit outfit = request.outfitId() != null
                    ? resolveOwnedOutfit(userId, request.outfitId())
                    : post.getOutfit();
            post.update(caption, outfit);
        }

        if (request.imageUrls() != null) {
            validateImageUrls(request.imageUrls());
            post.replaceImages(buildImages(post, request.imageUrls()));
        }

        if (request.hidden() != null) {
            post.setHidden(request.hidden());
        }

        return toFeedResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        FeedPost post = findOwnedPost(postId, userId);
        post.softDelete();
    }

    public FeedResponse getPost(Long postId, Long viewerUserId) {
        FeedPost post = feedPostRepository.findActiveWithDetailsById(postId)
                .orElseThrow(() -> new EntityNotFoundException("피드를 찾을 수 없습니다."));

        if (post.isHidden() && !Objects.equals(post.getAuthor().getId(), viewerUserId)) {
            throw new EntityNotFoundException("피드를 찾을 수 없습니다.");
        }

        return toFeedResponse(post, viewerUserId);
    }

    public FeedPageResponse getPublicFeed(int page, int size, Long viewerUserId) {
        Pageable pageable = PageRequest.of(page, normalizeSize(size));
        Page<FeedPost> posts = feedPostRepository.findPublicFeed(pageable);
        return mapFeedPage(posts, viewerUserId);
    }

    public FeedPageResponse getUserFeed(Long userId, int page, int size, Long viewerUserId) {
        findUser(userId);
        Pageable pageable = PageRequest.of(page, normalizeSize(size));
        Page<FeedPost> posts = feedPostRepository.findPublicFeedByAuthorId(userId, pageable);
        return mapFeedPage(posts, viewerUserId);
    }

    @Transactional
    public FeedImageUploadResponse uploadFeedImage(Long userId, MultipartFile file) {
        StoredImage storedImage = localImageStorageService.storeFeedPhoto(userId, file);
        return new FeedImageUploadResponse(storedImage.publicUrl());
    }

    @Transactional
    public FeedInteractionResponse toggleLike(Long postId, Long userId) {
        FeedPost post = findVisiblePost(postId, userId);
        User user = findUser(userId);

        return feedPostLikeRepository.findByUser_IdAndFeedPost_Id(userId, postId)
                .map(existing -> {
                    feedPostLikeRepository.delete(existing);
                    return new FeedInteractionResponse(false, feedPostLikeRepository.countByFeedPost_Id(postId));
                })
                .orElseGet(() -> {
                    feedPostLikeRepository.save(FeedPostLike.of(user, post));
                    return new FeedInteractionResponse(true, feedPostLikeRepository.countByFeedPost_Id(postId));
                });
    }

    @Transactional
    public FeedInteractionResponse toggleSave(Long postId, Long userId) {
        FeedPost post = findVisiblePost(postId, userId);
        User user = findUser(userId);

        if (post.getOutfit() == null) {
            throw new IllegalArgumentException("연결된 코디가 없어 저장할 수 없습니다.");
        }

        Outfit sourceOutfit = outfitRepository.findActiveByOutfitId(post.getOutfit().getOutfitId())
                .orElseThrow(() -> new EntityNotFoundException("코디를 찾을 수 없습니다."));

        return feedPostSaveRepository.findByUser_IdAndFeedPost_Id(userId, postId)
                .map(existing -> {
                    Outfit savedOutfit = existing.getSavedOutfit();
                    if (savedOutfit != null) {
                        savedOutfit.softDelete();
                    }
                    feedPostSaveRepository.delete(existing);
                    return new FeedInteractionResponse(false, feedPostSaveRepository.countByFeedPost_Id(postId));
                })
                .orElseGet(() -> {
                    Outfit clonedOutfit = outfitService.cloneOutfitToUserBook(sourceOutfit, userId);
                    boolean ownOutfit = sourceOutfit.getOutfitBook().getUser().getId().equals(userId);
                    Outfit savedOutfitRef = ownOutfit ? null : clonedOutfit;
                    feedPostSaveRepository.save(FeedPostSave.of(user, post, savedOutfitRef));
                    return new FeedInteractionResponse(true, feedPostSaveRepository.countByFeedPost_Id(postId));
                });
    }

    @Transactional
    public FeedCommentResponse createComment(Long postId, Long userId, FeedCommentRequest request) {
        FeedPost post = findVisiblePost(postId, userId);
        User author = findUser(userId);

        FeedComment parent = null;
        if (request.parentCommentId() != null) {
            parent = feedCommentRepository.findActiveByIdAndFeedPostId(request.parentCommentId(), postId)
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));
            if (parent.isReply()) {
                throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다.");
            }
        }

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(post, author, parent, request.content().trim())
        );
        return FeedCommentResponse.from(saved, List.of(), userId);
    }

    @Transactional
    public FeedCommentResponse updateComment(Long postId, Long commentId, Long userId, FeedCommentRequest request) {
        FeedComment comment = feedCommentRepository.findActiveByIdAndFeedPostId(commentId, postId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new AccessDeniedException("댓글을 수정할 권한이 없습니다.");
        }

        comment.updateContent(request.content().trim());
        return FeedCommentResponse.from(comment, List.of(), userId);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, Long userId) {
        FeedComment comment = feedCommentRepository.findActiveByIdAndFeedPostId(commentId, postId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }

        comment.softDelete();
    }

    public List<FeedCommentResponse> getComments(Long postId, Long viewerUserId) {
        findVisiblePost(postId, viewerUserId);

        List<FeedComment> comments = feedCommentRepository.findAllActiveByFeedPostId(postId);
        Map<Long, List<FeedCommentResponse>> repliesByParentId = new LinkedHashMap<>();

        for (FeedComment comment : comments) {
            if (comment.isReply()) {
                Long parentId = comment.getParent().getId();
                repliesByParentId
                        .computeIfAbsent(parentId, key -> new ArrayList<>())
                        .add(FeedCommentResponse.from(comment, List.of(), viewerUserId));
            }
        }

        return comments.stream()
                .filter(comment -> !comment.isReply())
                .map(comment -> FeedCommentResponse.from(
                        comment,
                        repliesByParentId.getOrDefault(comment.getId(), List.of()),
                        viewerUserId
                ))
                .toList();
    }

    @Transactional
    public FeedInteractionResponse followUser(Long followerId, Long followeeId) {
        if (Objects.equals(followerId, followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        User follower = findUser(followerId);
        User followee = findUser(followeeId);

        if (userFollowRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId)) {
            userFollowRepository.deleteByFollower_IdAndFollowee_Id(followerId, followeeId);
            return new FeedInteractionResponse(false, 0);
        }

        userFollowRepository.save(UserFollow.of(follower, followee));
        return new FeedInteractionResponse(true, 0);
    }

    private FeedPageResponse mapFeedPage(Page<FeedPost> posts, Long viewerUserId) {
        List<FeedResponse> content = posts.getContent().stream()
                .map(post -> toFeedResponse(post, viewerUserId))
                .toList();

        Page<FeedResponse> mapped = new org.springframework.data.domain.PageImpl<>(
                content,
                posts.getPageable(),
                posts.getTotalElements()
        );
        return FeedPageResponse.from(mapped);
    }

    private FeedResponse toFeedResponse(FeedPost post, Long viewerUserId) {
        long likeCount = feedPostLikeRepository.countByFeedPost_Id(post.getId());
        long commentCount = feedCommentRepository.countByFeedPost_IdAndDeletedAtIsNull(post.getId());
        boolean likedByMe = viewerUserId != null
                && feedPostLikeRepository.findByUser_IdAndFeedPost_Id(viewerUserId, post.getId()).isPresent();
        boolean savedByMe = viewerUserId != null
                && feedPostSaveRepository.findByUser_IdAndFeedPost_Id(viewerUserId, post.getId()).isPresent();

        OutfitResponse outfitResponse = null;
        if (post.getOutfit() != null) {
            outfitResponse = buildOutfitResponse(post.getOutfit(), post.getAuthor().getId());
        }

        return FeedResponse.of(post, outfitResponse, likeCount, commentCount, likedByMe, savedByMe, viewerUserId);
    }

    private OutfitResponse buildOutfitResponse(Outfit outfit, Long ownerUserId) {
        List<OutfitItem> items = outfitItemRepository.findAllByOutfit_OutfitId(outfit.getOutfitId());
        if (items.isEmpty()) {
            return OutfitResponse.from(outfit);
        }

        Map<Long, WardrobeClothes> wardrobeClothesByClothesId = findWardrobeClothesByClothesId(ownerUserId, items);
        List<OutfitItemResponse> itemResponses = items.stream()
                .map(item -> OutfitItemResponse.from(item, wardrobeClothesByClothesId.get(item.getClothes().getId())))
                .toList();
        return OutfitResponse.from(outfit, itemResponses);
    }

    private Map<Long, WardrobeClothes> findWardrobeClothesByClothesId(Long userId, List<OutfitItem> items) {
        List<Long> clothesIds = items.stream()
                .map(item -> item.getClothes().getId())
                .distinct()
                .toList();
        if (clothesIds.isEmpty()) {
            return Map.of();
        }
        return wardrobeClothesRepository.findAllByClothesIdsAndUserId(clothesIds, userId).stream()
                .collect(Collectors.toMap(wc -> wc.getClothes().getId(), wc -> wc, (left, right) -> left));
    }

    private FeedPost findOwnedPost(Long postId, Long userId) {
        return feedPostRepository.findActiveByIdAndAuthorId(postId, userId)
                .orElseThrow(() -> new EntityNotFoundException("피드를 찾을 수 없습니다."));
    }

    private FeedPost findVisiblePost(Long postId, Long viewerUserId) {
        FeedPost post = feedPostRepository.findActiveWithDetailsById(postId)
                .orElseThrow(() -> new EntityNotFoundException("피드를 찾을 수 없습니다."));

        if (post.isHidden() && !Objects.equals(post.getAuthor().getId(), viewerUserId)) {
            throw new EntityNotFoundException("피드를 찾을 수 없습니다.");
        }
        return post;
    }

    private Outfit resolveOwnedOutfit(Long userId, Long outfitId) {
        if (outfitId == null) {
            return null;
        }
        return outfitRepository.findActiveByOutfitIdAndUserId(outfitId, userId)
                .orElseThrow(() -> new EntityNotFoundException("연결할 코디를 찾을 수 없습니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void attachImages(FeedPost post, List<String> imageUrls) {
        validateImageUrls(imageUrls);
        post.replaceImages(buildImages(post, imageUrls));
    }

    private List<FeedPostImage> buildImages(FeedPost post, List<String> imageUrls) {
        List<FeedPostImage> images = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            images.add(FeedPostImage.of(post, imageUrls.get(index).trim(), index));
        }
        return images;
    }

    private void validateImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("피드 이미지는 최소 1장 이상 필요합니다.");
        }
        for (String imageUrl : imageUrls) {
            if (!StringUtils.hasText(imageUrl)) {
                throw new IllegalArgumentException("피드 이미지 URL이 비어 있습니다.");
            }
        }
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
