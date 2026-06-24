package com.closetnangam.be.domain.user.repository;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.status = :status
              AND u.withdrawnAt < :withdrawnAt
              AND (
                    u.email IS NULL
                    OR u.email NOT LIKE CONCAT(:anonymizedEmailPrefix, '%@deleted.closetnangam.local')
              )
            """)
    List<User> findExpiredWithdrawnUsersForCleanup(
            @Param("status") UserStatus status,
            @Param("withdrawnAt") LocalDateTime withdrawnAt,
            @Param("anonymizedEmailPrefix") String anonymizedEmailPrefix
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
