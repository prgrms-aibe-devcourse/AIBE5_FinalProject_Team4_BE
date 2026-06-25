package com.closetnangam.be.domain.user.repository;

import com.closetnangam.be.domain.user.entity.UserExternalLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserExternalLinkRepository extends JpaRepository<UserExternalLink, Long> {

    List<UserExternalLink> findAllByUserIdOrderBySortOrderAsc(Long userId);

    void deleteAllByUserId(Long userId);
}
