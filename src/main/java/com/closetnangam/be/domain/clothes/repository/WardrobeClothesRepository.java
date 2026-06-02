package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WardrobeClothesRepository extends JpaRepository<WardrobeClothes, Long> {

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.wardrobe w
            join fetch w.user
            join fetch wc.clothes c
            where wc.id = :wardrobeClothesId
            """)
    Optional<WardrobeClothes> findByIdWithDetails(@Param("wardrobeClothesId") Long wardrobeClothesId);

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
            order by c.createdAt desc
            """)
    List<WardrobeClothes> findAllByUserIdAndOwnershipStatus(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and wc.favorite = true
            order by wc.updatedAt desc
            """)
    List<WardrobeClothes> findFavoritesByUserIdAndOwnershipStatus(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id = :clothesId
            """)
    Optional<WardrobeClothes> findByClothesIdWithDetails(@Param("clothesId") Long clothesId);

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            left join fetch c.styleTags st
            left join fetch st.style
            where c.id = :clothesId and w.user.id = :userId
            """)
    Optional<WardrobeClothes> findByClothesIdAndUserId(
            @Param("clothesId") Long clothesId,
            @Param("userId") Long userId
    );

    void deleteByClothes_Id(Long clothesId);

    /**
     * 추천 후보 조회: 특정 사용자의 보유 옷 중 기준 옷과 다른 카테고리인 항목만 반환합니다.
     *
     * <p><b>fetch 전략</b><br>
     * {@code colorTags}와 {@code styleTags}를 동시에 {@code join fetch}하면
     * {@code MultipleBagFetchException}이 발생하므로, {@code styleTags} + {@code Style}만 이 쿼리에서
     * {@code left join fetch}로 로딩하고 {@code colorTags}는 {@code @Fetch(SUBSELECT)}로 별도 1쿼리 처리합니다.<br>
     * 결과: 이 쿼리 1회 + colorTags SUBSELECT 1회 (styleTags/Style은 join fetch에 포함).
     *
     * @param excludeClothesId 기준 옷 ID (결과에서 제외)
     * @param excludeCategory  기준 옷의 카테고리 코드 (동일 카테고리 제외)
     */
    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            left join fetch c.styleTags st
            left join fetch st.style
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and c.id <> :excludeClothesId
              and c.category <> :excludeCategory
            order by c.createdAt desc
            """)
    List<WardrobeClothes> findCandidatesForRecommendation(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus,
            @Param("excludeClothesId") Long excludeClothesId,
            @Param("excludeCategory") String excludeCategory
    );
}
