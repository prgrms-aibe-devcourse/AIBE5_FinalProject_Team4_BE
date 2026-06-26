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

    /**
     * 해당 옷이 공개 피드 게시물(hidden=false, 삭제되지 않음)의 코디 아이템으로 포함되어 있으면 true를 반환합니다.
     * DB 컬럼 추가 없이 피드 공개 여부를 공유 가능성의 기준으로 사용합니다.
     */
    @Query("""
            select case when count(oi) > 0 then true else false end
            from FeedPost fp
            join OutfitItem oi on oi.outfit = fp.outfit
            where fp.hidden = false
              and fp.deletedAt is null
              and oi.clothes.id = :clothesId
            """)
    boolean existsByClothesIdInPublicFeed(@Param("clothesId") Long clothesId);

    /**
     * 구매내역 캡처 이미지 URL이 공개 피드 코디 구성 옷에 연결되어 있으면 true.
     * {@code urlPattern} 예: {@code %/purchase-captures/3/capture.jpg}
     */
    @Query("""
            select case when count(oi) > 0 then true else false end
            from FeedPost fp
            join fp.outfit o
            join OutfitItem oi on oi.outfit = o
            join oi.clothes c
            join WardrobeClothes wc on wc.clothes = c and wc.deletedAt is null
            join wc.wardrobe w
            where fp.hidden = false
              and fp.deletedAt is null
              and w.user.id = :userId
              and (
                  wc.userImageUrl like :urlPattern
                  or c.imageUrl like :urlPattern
              )
            """)
    boolean existsPublicFeedByPurchaseCaptureImageUrl(
            @Param("userId") Long userId,
            @Param("urlPattern") String urlPattern
    );
}
