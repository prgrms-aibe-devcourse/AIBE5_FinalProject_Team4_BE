package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    /** clothes.info_source와 동기화되는 비정규화 컬럼(레거시). 빌더에서 clothes 기준으로만 설정합니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false, length = 50)
    private ClothesInfoSource registrationSource;

    @Column(name = "user_image_url", nullable = false, length = 500)
    private String userImageUrl;

    /** 사용자 옷장에서 제거된 시각. null이면 활성. {@link Clothes} 마스터 행은 유지됩니다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private WardrobeClothes(
            Wardrobe wardrobe,
            Clothes clothes,
            OwnershipStatus ownershipStatus,
            String size,
            String season,
            Boolean favorite,
            String userImageUrl
    ) {
        this.wardrobe = wardrobe;
        this.clothes = clothes;
        this.ownershipStatus = ownershipStatus;
        this.size = size;
        this.season = season;
        this.favorite = favorite != null ? favorite : false;
        this.userImageUrl = userImageUrl;
        if (clothes.getInfoSource() == null) {
            throw new IllegalArgumentException("옷 정보 출처가 설정되지 않았습니다.");
        }
        this.registrationSource = clothes.getInfoSource();
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
        this.registrationSource = this.clothes.getInfoSource();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 사용자 옷장에서만 제거합니다. 연관 {@link Clothes}·태그·피드 등 외부 참조용 데이터는 삭제하지 않습니다.
     */
    public void softDelete() {
        if (deletedAt != null) {
            throw new IllegalStateException("이미 삭제된 옷장 항목입니다.");
        }
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
