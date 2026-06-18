package com.closetnangam.be.domain.outfit.entity;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "outfit_styles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_outfit_styles_outfit_style",
                columnNames = {"outfit_id", "style_id"}
        )
)
public class OutfitStyles extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "style_role", nullable = false, length = 20)
    private StyleRole styleRole;

    @Column(name = "sort_order", nullable = false)
    private Byte sortOrder;

    private OutfitStyles(Outfit outfit, Style style, StyleRole styleRole, byte sortOrder) {
        this.outfit = outfit;
        this.style = style;
        this.styleRole = styleRole;
        this.sortOrder = sortOrder;
    }

    public static OutfitStyles create(Outfit outfit, Style style, StyleRole styleRole, byte sortOrder) {
        return new OutfitStyles(outfit, style, styleRole, sortOrder);
    }
}