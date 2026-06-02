package com.closetnangam.be.domain.catalog.entity;

import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@BatchSize(size = 100)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "styles")
public class Style extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "style_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Builder
    public Style(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static Style from(StyleCode styleCode) {
        return Style.builder()
                .code(styleCode.name())
                .name(styleCode.getLabel())
                .description(styleCode.getDescription())
                .build();
    }
}
