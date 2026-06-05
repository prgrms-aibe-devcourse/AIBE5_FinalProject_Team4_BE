package com.closetnangam.be.domain.user.repository;

import com.closetnangam.be.domain.user.entity.UserStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserStyleRepository extends JpaRepository<UserStyle, Long> {

    @Query("""
            select us
            from UserStyle us
            join fetch us.style
            where us.user.id = :userId
            """)
    List<UserStyle> findAllByUserId(@Param("userId") Long userId);

    Optional<UserStyle> findByUserIdAndStyleId(Long userId, Long styleId);
}
