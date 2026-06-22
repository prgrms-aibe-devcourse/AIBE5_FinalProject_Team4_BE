package com.closetnangam.be.domain.feed.repository;

import com.closetnangam.be.domain.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    @Query("""
            select fp
            from FeedPost fp
            join fetch fp.author
            left join fetch fp.outfit
            where fp.id = :postId
              and fp.deletedAt is null
            """)
    Optional<FeedPost> findActiveWithDetailsById(@Param("postId") Long postId);

    @Query(
            value = """
            select fp
            from FeedPost fp
            join fetch fp.author
            left join fetch fp.outfit
            where fp.deletedAt is null
              and fp.hidden = false
            order by fp.createdAt desc
            """,
            countQuery = """
            select count(fp)
            from FeedPost fp
            where fp.deletedAt is null
              and fp.hidden = false
            """
    )
    Page<FeedPost> findPublicFeed(Pageable pageable);

    @Query(
            value = """
            select fp
            from FeedPost fp
            join fetch fp.author
            left join fetch fp.outfit
            where fp.author.id = :userId
              and fp.deletedAt is null
              and fp.hidden = false
            order by fp.createdAt desc
            """,
            countQuery = """
            select count(fp)
            from FeedPost fp
            where fp.author.id = :userId
              and fp.deletedAt is null
              and fp.hidden = false
            """
    )
    Page<FeedPost> findPublicFeedByAuthorId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select fp
            from FeedPost fp
            join fetch fp.author
            left join fetch fp.outfit
            where fp.id = :postId
              and fp.author.id = :userId
              and fp.deletedAt is null
            """)
    Optional<FeedPost> findActiveByIdAndAuthorId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("""
            select count(fp)
            from FeedPost fp
            where fp.author.id = :userId
              and fp.deletedAt is null
              and fp.hidden = false
            """)
    long countPublicByAuthorId(@Param("userId") Long userId);
}
