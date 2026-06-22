package com.closetnangam.be.domain.feed.repository;

import com.closetnangam.be.domain.feed.entity.FeedPost;
import com.closetnangam.be.domain.feed.entity.FeedPostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedPostLikeRepository extends JpaRepository<FeedPostLike, Long> {

    Optional<FeedPostLike> findByUser_IdAndFeedPost_Id(Long userId, Long feedPostId);

    long countByFeedPost_Id(Long feedPostId);

    void deleteByUser_IdAndFeedPost_Id(Long userId, Long feedPostId);

    @Query(
            value = """
            select fp
            from FeedPostLike fpl
            join fpl.feedPost fp
            join fetch fp.author
            left join fetch fp.outfit
            where fpl.user.id = :userId
              and fp.deletedAt is null
              and fp.hidden = false
            order by fpl.createdAt desc
            """,
            countQuery = """
            select count(fpl)
            from FeedPostLike fpl
            join fpl.feedPost fp
            where fpl.user.id = :userId
              and fp.deletedAt is null
              and fp.hidden = false
            """
    )
    Page<FeedPost> findLikedFeedByUserId(@Param("userId") Long userId, Pageable pageable);
}
