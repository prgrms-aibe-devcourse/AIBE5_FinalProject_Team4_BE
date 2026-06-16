package com.closetnangam.be.domain.feed.repository;

import com.closetnangam.be.domain.feed.entity.FeedPostSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedPostSaveRepository extends JpaRepository<FeedPostSave, Long> {

    Optional<FeedPostSave> findByUser_IdAndFeedPost_Id(Long userId, Long feedPostId);

    long countByFeedPost_Id(Long feedPostId);

    void deleteByUser_IdAndFeedPost_Id(Long userId, Long feedPostId);
}
