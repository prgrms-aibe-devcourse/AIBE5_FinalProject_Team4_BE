package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.RegistrationSource;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wardrobe_clothes")
public class WardrobeClothes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wardrobe_clothes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wardrobe_id", nullable = false)
    private Wardrobe wardrobe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_status", nullable = false, length = 30)
    private OwnershipStatus ownershipStatus;

    @Column(nullable = false, length = 50)
    private String size;

    @Column(length = 50)
    private String season;

    @Column(nullable = false)
    private Boolean favorite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false, length = 50)
    private RegistrationSource registrationSource;

    @Column(name = "user_image_url", nullable = false, length = 500)
    private String userImageUrl;

    @Builder
    private WardrobeClothes(
            Wardrobe wardrobe,
            Clothes clothes,
            OwnershipStatus ownershipStatus,
            String size,
            String season,
            Boolean favorite,
            RegistrationSource registrationSource,
            String userImageUrl
    ) {
        this.wardrobe = wardrobe;
        this.clothes = clothes;
        this.ownershipStatus = ownershipStatus;
        this.size = size;
        this.season = season;
        this.favorite = favorite != null ? favorite : false;
        this.registrationSource = registrationSource;
        this.userImageUrl = userImageUrl;
    }

    public void updateFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public void updateWardrobeDetails(String size, String season, String userImageUrl) {
        this.size = size;
        this.season = season;
        this.userImageUrl = userImageUrl;
    }

    public void convertToOwned(String size, String season, String userImageUrl) {
        this.ownershipStatus = OwnershipStatus.OWNED;
        this.size = size;
        this.season = season;
        this.userImageUrl = userImageUrl;
    }
}
