package com.closetnangam.be.domain.feed.controller;

import com.closetnangam.be.domain.feed.dto.request.FeedCommentRequest;
import com.closetnangam.be.domain.feed.dto.request.FeedCreateRequest;
import com.closetnangam.be.domain.feed.dto.request.FeedUpdateRequest;
import com.closetnangam.be.domain.feed.dto.response.FeedCommentResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedImageUploadResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedInteractionResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedPageResponse;
import com.closetnangam.be.domain.feed.dto.response.FeedResponse;
import com.closetnangam.be.domain.feed.service.FeedService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Feed", description = "룩피드 API (FEED-001~008)")
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "피드 업로드", description = "FEED-001. 피드 사진과 저장한 코디를 연결해 룩피드에 게시합니다.")
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<FeedResponse>> createPost(@Valid @RequestBody FeedCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(feedService.createPost(userId, request)));
    }

    @Operation(summary = "공유 코디 목록", description = "FEED-002. 공개 피드 목록을 최신순으로 조회합니다.")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<FeedPageResponse>> getPublicFeed(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.getPublicFeed(page, size, userId)));
    }

    @Operation(summary = "코디 공유 상세", description = "FEED-003. 피드 상세와 연결 코디 정보를 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<FeedResponse>> getPost(@PathVariable @Min(1) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.getPost(postId, userId)));
    }

    @Operation(summary = "피드 수정", description = "작성자만 피드 내용·이미지·연결 코디·숨김 상태를 수정합니다.")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<FeedResponse>> updatePost(
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody FeedUpdateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.updatePost(postId, userId, request)));
    }

    @Operation(summary = "피드 삭제", description = "작성자만 피드를 소프트 삭제합니다.")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable @Min(1) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        feedService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "사용자 피드 목록", description = "특정 사용자가 공유한 공개 피드 목록을 조회합니다.")
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ApiResponse<FeedPageResponse>> getUserFeed(
            @PathVariable @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        Long viewerUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.getUserFeed(userId, page, size, viewerUserId)));
    }

    @Operation(summary = "피드 이미지 업로드", description = "피드 작성 전 이미지를 업로드하고 URL을 반환합니다.")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FeedImageUploadResponse>> uploadFeedImage(
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(feedService.uploadFeedImage(userId, file)));
    }

    @Operation(summary = "좋아요 토글", description = "FEED-004. 피드 좋아요를 추가하거나 취소합니다.")
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<FeedInteractionResponse>> toggleLike(@PathVariable @Min(1) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.toggleLike(postId, userId)));
    }

    @Operation(summary = "저장 토글", description = "FEED-005. 피드를 저장하거나 저장을 취소합니다.")
    @PostMapping("/posts/{postId}/saves")
    public ResponseEntity<ApiResponse<FeedInteractionResponse>> toggleSave(@PathVariable @Min(1) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.toggleSave(postId, userId)));
    }

    @Operation(summary = "댓글 목록", description = "FEED-006/007. 피드 댓글과 대댓글을 조회합니다.")
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<FeedCommentResponse>>> getComments(@PathVariable @Min(1) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.getComments(postId, userId)));
    }

    @Operation(summary = "댓글 작성", description = "FEED-006/007. 피드 댓글 또는 대댓글을 작성합니다.")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<FeedCommentResponse>> createComment(
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody FeedCommentRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(feedService.createComment(postId, userId, request)));
    }

    @Operation(summary = "댓글 삭제", description = "작성자만 댓글을 소프트 삭제합니다.")
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        feedService.deleteComment(postId, commentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "팔로우 토글", description = "FEED-008. 다른 사용자를 팔로우하거나 취소합니다.")
    @PostMapping("/users/{followeeId}/follows")
    public ResponseEntity<ApiResponse<FeedInteractionResponse>> toggleFollow(@PathVariable @Min(1) Long followeeId) {
        Long followerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(feedService.followUser(followerId, followeeId)));
    }
}
