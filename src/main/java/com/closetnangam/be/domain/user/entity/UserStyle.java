package com.closetnangam.be.domain.user.entity;

import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_styles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_styles_user_style",
                columnNames = {"user_id", "style_code"}
        )
)
public class UserStyle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "style_code", nullable = false, length = 30)
    private StyleCode styleCode;

    @Column(name = "preference_weight", nullable = false)
    private Integer preferenceWeight = 0;

    @Column(name = "wardrobe_weight", nullable = false)
    private Integer wardrobeWeight = 0;

    @Column(name = "combined_weight", nullable = false)
    private Integer combinedWeight = 0;

    @Builder
    public UserStyle(User user, StyleCode styleCode) {
        this.user = user;
        this.styleCode = styleCode;
        this.preferenceWeight = 0;
        this.wardrobeWeight = 0;
        this.combinedWeight = 0;
    }

    public void updatePreferenceWeight(int weight) {
        this.preferenceWeight = weight;
        calculateCombinedWeight();
    }

    public void updateWardrobeWeight(int weight) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight();
    }

    public void syncWardrobeWeight(int weight) {
        this.wardrobeWeight = weight;
        calculateCombinedWeight();
    }

    private void calculateCombinedWeight() {
        this.combinedWeight = this.preferenceWeight + this.wardrobeWeight;
    }
}
