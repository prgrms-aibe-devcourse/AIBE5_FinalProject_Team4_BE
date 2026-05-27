package com.closetnangam.be.domain.clothes.entity;

import com.closetnangam.be.domain.catalog.entity.Style;
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
        name = "clothing_styles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clothing_styles_clothes_style",
                columnNames = {"clothes_id", "style_id"}
        )
)
public class ClothesStyleTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    private ClothesStyleTag(Clothes clothes, Style style) {
        this.clothes = clothes;
        this.style = style;
    }

    public static ClothesStyleTag create(Clothes clothes, Style style) {
        return new ClothesStyleTag(clothes, style);
    }
}
