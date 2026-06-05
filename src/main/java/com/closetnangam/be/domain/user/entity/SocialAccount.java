package com.closetnangam.be.domain.user.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_accounts_provider",
                columnNames = {"provider", "provider_user_id"}
        )
)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", nullable = false, length = 255)
    private String providerEmail;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Builder
    private SocialAccount(
            User user,
            String provider,
            String providerUserId,
            String providerEmail,
            LocalDateTime lastLoginAt
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.lastLoginAt = lastLoginAt != null ? lastLoginAt : LocalDateTime.now();
    }

    public void recordLogin(String providerEmail) {
        if (providerEmail != null && !providerEmail.isBlank()) {
            this.providerEmail = providerEmail;
        }
        this.lastLoginAt = LocalDateTime.now();
    }
}
