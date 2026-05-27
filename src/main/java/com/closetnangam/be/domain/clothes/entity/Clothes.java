package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.clothes.enums.SourceType;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clothes")
public class Clothes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wardrobe_id", nullable = false)
    private Wardrobe wardrobe;

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

    @Column(nullable = false, length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Column(name = "external_source", nullable = false, length = 50)
    private String externalSource;

    @Column(name = "external_product_id", nullable = false)
    private String externalProductId;

    @Column(name = "external_product_url", nullable = false, length = 500)
    private String externalProductUrl;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesStyleTag> styleTags = new ArrayList<>();

    @Builder
    private Clothes(
            Wardrobe wardrobe,
            String name,
            String brandName,
            String productCode,
            String imageUrl,
            String category,
            String itemType,
            String color,
            SourceType sourceType,
            String externalSource,
            String externalProductId,
            String externalProductUrl,
            Boolean isVerified
    ) {
        this.wardrobe = wardrobe;
        this.name = name;
        this.brandName = brandName;
        this.productCode = productCode;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemType = itemType;
        this.color = color;
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
            String color,
            Boolean isVerified
    ) {
        this.name = name;
        this.brandName = brandName;
        this.productCode = productCode;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemType = itemType;
        this.color = color;
        this.isVerified = isVerified;
    }

    public void replaceStyleTags(List<ClothesStyleTag> newStyleTags) {
        this.styleTags.clear();
        this.styleTags.addAll(newStyleTags);
    }

    public void addStyleTag(ClothesStyleTag styleTag) {
        this.styleTags.add(styleTag);
    }
}
