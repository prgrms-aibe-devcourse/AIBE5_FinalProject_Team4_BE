package com.closetnangam.be.domain.feed.repository;

import com.closetnangam.be.domain.feed.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    boolean existsByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    void deleteByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);
}
