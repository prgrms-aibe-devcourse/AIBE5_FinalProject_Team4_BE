package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "clothing_styles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clothing_styles_clothes_style",
                columnNames = {"clothes_id", "style_id"}
        )
)
public class ClothesStyleTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    /**
     * 참조 Style 엔티티. LAZY 유지 — 추천 쿼리에서 {@code left join fetch st.style}로 함께 로딩됩니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "style_role", nullable = false, length = 20)
    private StyleRole styleRole;

    @Column(name = "sort_order", nullable = false)
    private Byte sortOrder;

    private ClothesStyleTag(Clothes clothes, Style style, StyleRole styleRole, byte sortOrder) {
        this.clothes = clothes;
        this.style = style;
        this.styleRole = styleRole;
        this.sortOrder = sortOrder;
    }

    public static ClothesStyleTag create(Clothes clothes, Style style, StyleRole styleRole, byte sortOrder) {
        return new ClothesStyleTag(clothes, style, styleRole, sortOrder);
    }
}
