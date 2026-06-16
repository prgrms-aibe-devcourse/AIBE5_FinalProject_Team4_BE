package com.closetnangam.be.domain.feed.entity;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feed_comments")
public class FeedComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_post_id", nullable = false)
    private FeedPost feedPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private FeedComment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private FeedComment(FeedPost feedPost, User author, FeedComment parent, String content) {
        this.feedPost = feedPost;
        this.author = author;
        this.parent = parent;
        this.content = content;
    }

    public static FeedComment create(FeedPost feedPost, User author, FeedComment parent, String content) {
        return FeedComment.builder()
                .feedPost(feedPost)
                .author(author)
                .parent(parent)
                .content(content)
                .build();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isReply() {
        return parent != null;
    }
}
