package com.closetnangam.be.domain.feed.entity;

import com.closetnangam.be.domain.outfit.entity.Outfit;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "feed_post_saves",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_feed_post_saves_user_post",
                columnNames = {"user_id", "feed_post_id"}
        )
)
public class FeedPostSave extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_post_save_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_post_id", nullable = false)
    private FeedPost feedPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_outfit_id")
    private Outfit savedOutfit;

    public static FeedPostSave of(User user, FeedPost feedPost) {
        FeedPostSave save = new FeedPostSave();
        save.user = user;
        save.feedPost = feedPost;
        return save;
    }

    public static FeedPostSave of(User user, FeedPost feedPost, Outfit savedOutfit) {
        FeedPostSave save = of(user, feedPost);
        save.savedOutfit = savedOutfit;
        return save;
    }
}
