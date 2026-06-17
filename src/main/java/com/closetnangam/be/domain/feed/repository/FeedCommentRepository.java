package com.closetnangam.be.domain.feed.repository;

import com.closetnangam.be.domain.feed.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    @Query("""
            select c
            from FeedComment c
            join fetch c.author
            where c.feedPost.id = :postId
              and c.deletedAt is null
            order by c.createdAt asc
            """)
    List<FeedComment> findAllActiveByFeedPostId(@Param("postId") Long postId);

    @Query("""
            select c
            from FeedComment c
            join fetch c.author
            where c.id = :commentId
              and c.feedPost.id = :postId
              and c.deletedAt is null
            """)
    Optional<FeedComment> findActiveByIdAndFeedPostId(
            @Param("commentId") Long commentId,
            @Param("postId") Long postId
    );

    long countByFeedPost_IdAndDeletedAtIsNull(Long feedPostId);
}
