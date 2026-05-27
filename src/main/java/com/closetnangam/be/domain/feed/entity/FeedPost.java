package com.closetnangam.be.domain.feed.entity;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_posts")
public class FeedPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // TODO: Outfit 엔티티 구현 후 @ManyToOne 관계로 변경
    @Column(name = "outfit_id", nullable = false)
    private Long outfitId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private FeedPost(User user, Long outfitId, String content) {
        this.user = user;
        this.outfitId = outfitId;
        this.content = content;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
