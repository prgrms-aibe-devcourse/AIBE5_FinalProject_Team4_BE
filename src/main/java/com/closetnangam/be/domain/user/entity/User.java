package com.closetnangam.be.domain.user.entity;

import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(nullable = false)
    private Boolean withdrawn = false;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    @Builder
    public User(String nickname, String email, String profileImage,
                Gender gender, String birthDate) {
        this.nickname = nickname;
        this.email = email;
        this.profileImage = profileImage;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public void updateProfile(String nickname, String profileImage,
                              Gender gender, String birthDate) {
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public void withdraw() {
        this.withdrawn = true;
        this.withdrawnAt = LocalDateTime.now();
    }
}