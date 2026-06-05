package com.closetnangam.be.domain.user.repository;

import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.user.entity.UserStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStyleRepository extends JpaRepository<UserStyle, Long> {
    List<UserStyle> findAllByUserId(Long userId);
    Optional<UserStyle> findByUserIdAndStyleCode(Long userId, StyleCode styleCode);
}
