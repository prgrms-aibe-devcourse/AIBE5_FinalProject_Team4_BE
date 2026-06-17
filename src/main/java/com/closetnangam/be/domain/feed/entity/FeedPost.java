package com.closetnangam.be.domain.feed.entity;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id")
    private Outfit outfit;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String caption;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @BatchSize(size = 20)
    @OneToMany(mappedBy = "feedPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<FeedPostImage> images = new ArrayList<>();

    @Builder
    private FeedPost(User author, Outfit outfit, String caption, boolean hidden) {
        this.author = author;
        this.outfit = outfit;
        this.caption = caption != null ? caption : "";
        this.hidden = hidden;
    }

    public static FeedPost create(User author, Outfit outfit, String caption) {
        return FeedPost.builder()
                .author(author)
                .outfit(outfit)
                .caption(caption)
                .hidden(false)
                .build();
    }

    public void update(String caption, Outfit outfit) {
        this.caption = caption != null ? caption : "";
        this.outfit = outfit;
    }

    public void replaceImages(List<FeedPostImage> newImages) {
        this.images.clear();
        this.images.addAll(newImages);
    }

    public void addImage(FeedPostImage image) {
        this.images.add(image);
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
