package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.clothes.enums.ColorRole;
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
        name = "clothing_colors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clothing_colors_clothes_role_order",
                columnNames = {"clothes_id", "color_role", "sort_order"}
        )
)
public class ClothingColor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_color_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Column(name = "color_code", nullable = false, length = 50)
    private String colorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_role", nullable = false, length = 20)
    private ColorRole colorRole;

    @Column(name = "sort_order", nullable = false)
    private Byte sortOrder;

    private ClothingColor(Clothes clothes, String colorCode, ColorRole colorRole, byte sortOrder) {
        this.clothes = clothes;
        this.colorCode = colorCode;
        this.colorRole = colorRole;
        this.sortOrder = sortOrder;
    }

    public static ClothingColor create(Clothes clothes, String colorCode, ColorRole colorRole, byte sortOrder) {
        return new ClothingColor(clothes, colorCode, colorRole, sortOrder);
    }
}
