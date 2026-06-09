package com.closetnangam.be.domain.clothes.entity;


import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clothes")
public class Clothes extends BaseEntity {

    public static final String EXTERNAL_NONE = "NONE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothes_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_gender", nullable = false, length = 20)
    private ClothesGender targetGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "clothes_info_source", nullable = false, length = 50)
    private ClothesInfoSource clothesInfoSource;

    @Column(name = "external_source", nullable = false, length = 50)
    private String externalSource;

    @Column(name = "external_product_id", nullable = false, length = 255)
    private String externalProductId;

    @Column(name = "external_product_url", nullable = false, length = 500)
    private String externalProductUrl;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * 색상 태그 컬렉션.
     * 외부 코드는 {@link #getSortedColorTags()}를 통해 접근합니다.
     * {@code @Fetch(SUBSELECT)}는 컬렉션 로딩 방식만 지정하며, 최초 접근 시점까지 LAZY를 유지합니다.
     */
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClothingColor> colorTags = new ArrayList<>();

    /**
     * 스타일 태그 컬렉션.
     * 외부 코드는 {@link #getSortedStyleTags()}를 통해 접근합니다.
     */
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClothesStyleTag> styleTags = new ArrayList<>();

    @Builder
    private Clothes(
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            ClothesGender targetGender,
            ClothesInfoSource clothesInfoSource,
            String externalSource,
            String externalProductId,
            String externalProductUrl,
            Boolean isVerified
    ) {
        this.name = name;
        this.brandName = brandName;
        this.productCode = productCode;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemType = itemType;
        this.targetGender = targetGender != null ? targetGender : ClothesGender.UNISEX;
        if (clothesInfoSource == null) {
            throw new IllegalArgumentException("옷 정보 출처는 필수입니다.");
        }
        this.clothesInfoSource = clothesInfoSource;
        this.externalSource = externalSource;
        this.externalProductId = externalProductId;
        this.externalProductUrl = externalProductUrl;
        this.isVerified = isVerified;
    }

    public void update(
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            ClothesGender targetGender,
            Boolean isVerified
    ) {
        this.name = name;
        this.brandName = brandName;
        this.productCode = productCode;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemType = itemType;
        this.targetGender = targetGender != null ? targetGender : ClothesGender.UNISEX;
        this.isVerified = isVerified;
    }

    public void replaceColorTags(List<ClothingColor> newColorTags) {
        synchronized (this) {
            this.colorTags.clear();
            this.colorTags.addAll(newColorTags);
            sortedColorTagsCache = null;
            recommendationTagSnapshotCache = null;
        }
    }

    public void replaceStyleTags(List<ClothesStyleTag> newStyleTags) {
        synchronized (this) {
            this.styleTags.clear();
            this.styleTags.addAll(newStyleTags);
            sortedStyleTagsCache = null;
            recommendationTagSnapshotCache = null;
        }
    }

    public void addColorTag(ClothingColor colorTag) {
        synchronized (this) {
            this.colorTags.add(colorTag);
            sortedColorTagsCache = null;
            recommendationTagSnapshotCache = null;
        }
    }

    public void addStyleTag(ClothesStyleTag styleTag) {
        synchronized (this) {
            this.styleTags.add(styleTag);
            sortedStyleTagsCache = null;
            recommendationTagSnapshotCache = null;
        }
    }

    private transient volatile List<ClothingColor> sortedColorTagsCache;
    private transient volatile List<ClothesStyleTag> sortedStyleTagsCache;
    private transient volatile ClothesTagSnapshot recommendationTagSnapshotCache;

    public ClothesTagSnapshot getRecommendationTagSnapshot() {
        ClothesTagSnapshot cached = recommendationTagSnapshotCache;
        if (cached == null) {
            synchronized (this) {
                cached = recommendationTagSnapshotCache;
                if (cached == null) {
                    cached = ClothesTagSnapshot.from(this);
                    recommendationTagSnapshotCache = cached;
                }
            }
        }
        return cached;
    }

    /**
     * 정렬된 색상 태그 목록. {@link com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot} 등
     * 외부 코드는 이 메서드로만 colorTags 에 접근해야 합니다.
     */
    public List<ClothingColor> getSortedColorTags() {
        List<ClothingColor> cached = sortedColorTagsCache;
        if (cached == null) {
            synchronized (this) {
                cached = sortedColorTagsCache;
                if (cached == null) {
                    cached = colorTags.stream()
                            .sorted(Comparator.comparing(ClothingColor::getSortOrder))
                            .toList();
                    sortedColorTagsCache = cached;
                }
            }
        }
        return cached;
    }

    /**
     * 정렬된 스타일 태그 목록. {@link com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot} 등
     * 외부 코드는 이 메서드로만 styleTags 에 접근해야 합니다.
     */
    public List<ClothesStyleTag> getSortedStyleTags() {
        List<ClothesStyleTag> cached = sortedStyleTagsCache;
        if (cached == null) {
            synchronized (this) {
                cached = sortedStyleTagsCache;
                if (cached == null) {
                    cached = styleTags.stream()
                            .sorted(Comparator.comparing(ClothesStyleTag::getSortOrder))
                            .toList();
                    sortedStyleTagsCache = cached;
                }
            }
        }
        return cached;
    }

    private void invalidateSortedTagCaches() {
        synchronized (this) {
            sortedColorTagsCache = null;
            sortedStyleTagsCache = null;
            recommendationTagSnapshotCache = null;
        }
    }

    public void convertToOwned(String productCode, Boolean isVerified) {
        this.clothesInfoSource = ClothesInfoSource.PURCHASE_HISTORY;
        this.externalSource = EXTERNAL_NONE;
        this.externalProductId = EXTERNAL_NONE;
        this.externalProductUrl = EXTERNAL_NONE;
        this.productCode = productCode;
        this.isVerified = isVerified;
    }
}