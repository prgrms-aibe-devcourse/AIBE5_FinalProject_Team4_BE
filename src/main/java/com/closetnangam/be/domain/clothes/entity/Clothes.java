package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.clothes.enums.SourceType;
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

    private static final String EXTERNAL_NONE = "NONE";

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
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

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

    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClothingColor> colorTags = new ArrayList<>();

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
            SourceType sourceType,
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
        this.sourceType = sourceType;
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
            Boolean isVerified
    ) {
        this.name = name;
        this.brandName = brandName;
        this.productCode = productCode;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemType = itemType;
        this.isVerified = isVerified;
    }

    public void replaceColorTags(List<ClothingColor> newColorTags) {
        this.colorTags.clear();
        this.colorTags.addAll(newColorTags);
    }

    public void replaceStyleTags(List<ClothesStyleTag> newStyleTags) {
        this.styleTags.clear();
        this.styleTags.addAll(newStyleTags);
    }

    public void addColorTag(ClothingColor colorTag) {
        this.colorTags.add(colorTag);
    }

    public void addStyleTag(ClothesStyleTag styleTag) {
        this.styleTags.add(styleTag);
    }

    public List<ClothingColor> getSortedColorTags() {
        return colorTags.stream()
                .sorted(Comparator.comparing(ClothingColor::getSortOrder))
                .toList();
    }

    public List<ClothesStyleTag> getSortedStyleTags() {
        return styleTags.stream()
                .sorted(Comparator.comparing(ClothesStyleTag::getSortOrder))
                .toList();
    }

    public void convertToOwned(String productCode, Boolean isVerified) {
        if (this.sourceType != SourceType.WISHLIST) {
            throw new IllegalArgumentException("미보유 옷만 보유 옷으로 전환할 수 있습니다.");
        }
        this.sourceType = SourceType.OWNED;
        this.externalSource = EXTERNAL_NONE;
        this.externalProductId = EXTERNAL_NONE;
        this.externalProductUrl = EXTERNAL_NONE;
        this.productCode = productCode;
        this.isVerified = isVerified;
    }
}
