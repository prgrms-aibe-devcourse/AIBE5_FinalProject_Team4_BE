package com.closetnangam.be.domain.user.entity;

import com.closetnangam.be.domain.user.enums.UserStatus;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    private static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDateTime INACTIVE_TIMESTAMP = LocalDateTime.of(1970, 1, 1, 0, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", nullable = false, length = 500)
    private String profileImageUrl;

    @Column(name = "profile_bio", nullable = false, length = 255)
    private String profileBio;

    @Column(name = "external_link_url", nullable = false, length = 255)
    private String externalLinkUrl;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "region_code", nullable = false, length = 50)
    private String regionCode;

    @Column(name = "marketing_agreed", nullable = false)
    private Boolean marketingAgreed;

    @Column(name = "marketing_agreed_at", nullable = false)
    private LocalDateTime marketingAgreedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    @Builder
    public User(
            String nickname,
            String email,
            String profileImageUrl,
            String profileBio,
            String externalLinkUrl,
            Gender gender,
            LocalDate birthDate,
            String regionName,
            String regionCode,
            Boolean marketingAgreed,
            LocalDateTime marketingAgreedAt,
            UserStatus status
    ) {
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.profileBio = profileBio;
        this.externalLinkUrl = externalLinkUrl;
        this.gender = gender;
        this.birthDate = birthDate;
        this.regionName = regionName;
        this.regionCode = regionCode;
        this.marketingAgreed = marketingAgreed;
        this.marketingAgreedAt = marketingAgreedAt;
        this.status = status;
    }

    @PrePersist
    void applyDefaults() {
        if (profileImageUrl == null) {
            profileImageUrl = "";
        }
        if (profileBio == null) {
            profileBio = "";
        }
        if (externalLinkUrl == null) {
            externalLinkUrl = "";
        }
        if (birthDate == null) {
            birthDate = DEFAULT_BIRTH_DATE;
        }
        if (gender == null) {
            gender = Gender.OTHER;
        }
        if (regionName == null) {
            regionName = "";
        }
        if (regionCode == null) {
            regionCode = "";
        }
        if (marketingAgreed == null) {
            marketingAgreed = false;
        }
        if (marketingAgreedAt == null) {
            marketingAgreedAt = INACTIVE_TIMESTAMP;
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (withdrawnAt == null) {
            withdrawnAt = INACTIVE_TIMESTAMP;
        }
    }

    public void updateProfile(
            String nickname,
            String profileImageUrl,
            String profileBio,
            String externalLinkUrl,
            Gender gender,
            LocalDate birthDate,
            String regionName,
            String regionCode
    ) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.profileBio = profileBio;
        this.externalLinkUrl = externalLinkUrl;
        this.gender = gender;
        this.birthDate = birthDate;
        this.regionName = regionName;
        this.regionCode = regionCode;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public void restore() {
        this.status = UserStatus.ACTIVE;
        this.withdrawnAt = INACTIVE_TIMESTAMP;
    }

    public boolean isOnboarded() {
        return !DEFAULT_BIRTH_DATE.equals(this.birthDate);
    }

    public void updateMarketingAgreement(boolean marketingAgreed) {
        this.marketingAgreed = marketingAgreed;
        this.marketingAgreedAt = marketingAgreed ? LocalDateTime.now() : INACTIVE_TIMESTAMP;
    }

    public String getDefaultAnchorItemType() {
        if (this.gender == Gender.FEMALE) {
            return "SKIRT";
        }
        return "SHORT_SLEEVE";
    }
}
