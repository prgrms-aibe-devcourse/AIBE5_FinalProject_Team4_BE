package com.closetnangam.be.domain.wardrobe.entity;

import com.closetnangam.be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "wardrobes",
        uniqueConstraints = @UniqueConstraint(name = "uk_wardrobes_user_id", columnNames = "user_id")
)
public class Wardrobe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wardrobe_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 옷장 통계(user_styles.wardrobe_weight)가 마지막으로 동기화된 시각. null이면 아직 동기화되지 않음. */
    @Column(name = "statistics_synced_at")
    private LocalDateTime statisticsSyncedAt;

    @Builder
    private Wardrobe(User user) {
        this.user = user;
    }

    public static Wardrobe create(User user) {
        return Wardrobe.builder()
                .user(user)
                .build();
    }

    public void markStatisticsSynced(LocalDateTime syncedAt) {
        this.statisticsSyncedAt = syncedAt;
    }
}
