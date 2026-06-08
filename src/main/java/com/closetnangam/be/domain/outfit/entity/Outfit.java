package com.closetnangam.be.domain.outfit.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outfits")
public class Outfit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_id")
    private Long outfitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_book_id", nullable = false)
    private OutfitBook outfitBook;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 50)
    private String situation;

    @Column(nullable = false, length = 50)
    private String season;

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Outfit(
            OutfitBook outfitBook,
            String title,
            String description,
            String thumbnailUrl,
            String situation,
            String season,
            boolean favorite
    ) {
        this.outfitBook = outfitBook;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.situation = situation;
        this.season = season;
        this.favorite = favorite;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void markFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void update(
            String title,
            String description,
            String thumbnailUrl,
            String situation,
            String season,
            boolean favorite
    ) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.situation = situation;
        this.season = season;
        this.favorite = favorite;
    }
}
