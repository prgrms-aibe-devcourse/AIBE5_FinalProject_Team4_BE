package com.closetnangam.be.domain.outfit.entity;

import com.closetnangam.be.domain.clothes.entity.Clothes;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outfit_items")
public class OutfitItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Column(name = "item_role", nullable = false, length = 30)
    private String itemRole;

    @Column(name = "layer_order", nullable = false)
    private Integer layerOrder;

    @Builder
    private OutfitItem(Outfit outfit, Clothes clothes, String itemRole, Integer layerOrder) {
        this.outfit = outfit;
        this.clothes = clothes;
        this.itemRole = itemRole;
        this.layerOrder = layerOrder;
    }
}
