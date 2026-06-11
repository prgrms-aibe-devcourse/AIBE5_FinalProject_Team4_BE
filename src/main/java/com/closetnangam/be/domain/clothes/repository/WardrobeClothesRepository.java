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
              and wc.deletedAt is null
            """)
    Optional<WardrobeClothes> findByIdWithDetails(@Param("wardrobeClothesId") Long wardrobeClothesId);

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and wc.deletedAt is null
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
              and wc.deletedAt is null
            order by wc.updatedAt desc
            """)
    List<WardrobeClothes> findFavoritesByUserIdAndOwnershipStatus(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    /**
     * {@code clothes_id} 기준 옷장 연결 조회. 소프트 딜리트된 항목도 포함합니다.
     *
     * <p>사용자 노출 용도에는 사용 금지. 피드·추천 등 {@link com.closetnangam.be.domain.clothes.entity.Clothes}
     * 마스터 데이터만 필요한 경우 {@link ClothesRepository#findById}를 우선 사용하세요.</p>
     *
     * <p><b>Orphan 정책</b>: {@code WardrobeClothes} 소프트 딜리트 후 해당 {@code Clothes}를 참조하는
     * 활성 행이 없어도 마스터 {@code Clothes}는 보존됩니다. 필요 시 별도 배치로 정리하세요.</p>
     */
    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id = :clothesId
            """)
    Optional<WardrobeClothes> findByClothesIdWithDetailsIncludeDeleted(@Param("clothesId") Long clothesId);

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            left join fetch c.styleTags st
            left join fetch st.style
            where c.id = :clothesId
              and w.user.id = :userId
              and wc.deletedAt is null
            """)
    Optional<WardrobeClothes> findByClothesIdAndUserId(
            @Param("clothesId") Long clothesId,
            @Param("userId") Long userId
    );

    /**
     * 사용자·옷 기준 옷장 연결 1건 조회. {@code deletedAt} 필터 없음 — 활성·소프트 삭제 모두 반환 가능.
     *
     * <p>위시리스트 재등록 등 “과거 연결 복원” 용도 전용.
     * 일반 조회는 {@link #findByClothesIdAndUserId}를 사용하세요.</p>
     */
    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id = :clothesId
              and w.user.id = :userId
            """)
    Optional<WardrobeClothes> findByClothesIdAndUserIdIgnoringSoftDelete(
            @Param("clothesId") Long clothesId,
            @Param("userId") Long userId
    );

    /**
     * 저장된 코디 구성 옷을 응답으로 복원할 때, 사용자 옷장에 실제로 연결된 보유 옷 정보를 함께 채우기 위한 조회.
     *
     * 외부 쇼핑 상품처럼 사용자 옷장에 연결되지 않은 Clothes는 이 결과에 포함되지 않는다.
     */
    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id in :clothesIds
              and w.user.id = :userId
              and wc.deletedAt is null
            """)
    List<WardrobeClothes> findAllByClothesIdsAndUserId(
            @Param("clothesIds") List<Long> clothesIds,
            @Param("userId") Long userId
    );

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
              and wc.deletedAt is null
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

    @Query("""
            select c.id from WardrobeClothes wc
            join wc.clothes c
            join wc.wardrobe w
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and wc.deletedAt is null
            """)
    List<Long> findOwnedClothesIdsByUserId(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            where w.user.id = :userId
              and wc.deletedAt is null
              and wc.ownershipStatus in :ownershipStatuses
            """)
    List<WardrobeClothes> findAllActiveByUserIdAndOwnershipStatuses(
            @Param("userId") Long userId,
            @Param("ownershipStatuses") List<OwnershipStatus> ownershipStatuses
    );

    @Query("""
            select case when count(wc) > 0 then true else false end
            from WardrobeClothes wc
            join wc.clothes c
            join wc.wardrobe w
            where w.user.id = :userId
              and wc.deletedAt is null
              and c.externalSource = :externalSource
              and c.externalProductId = :externalProductId
              and c.externalProductId <> 'NONE'
            """)
    boolean existsActiveByUserIdAndExternalProduct(
            @Param("userId") Long userId,
            @Param("externalSource") String externalSource,
            @Param("externalProductId") String externalProductId
    );

    /**
     * 옷장 통계용: 보유 옷 + 스타일 태그 + Style 을 한 번에 로딩합니다.
     */
    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            left join fetch c.styleTags st
            left join fetch st.style
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and wc.deletedAt is null
            """)
    List<WardrobeClothes> findOwnedForStatistics(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user u
            left join fetch c.styleTags st
            left join fetch st.style
            where w.id = :wardrobeId
              and wc.deletedAt is null
            """)
    List<WardrobeClothes> findAllByWardrobeId(@Param("wardrobeId") Long wardrobeId);

    @Query("""
            select case when count(wc) > 0 then true else false end
            from WardrobeClothes wc
            join wc.clothes c
            join wc.wardrobe w
            where w.user.id = :userId
              and wc.deletedAt is null
              and c.productCode = :productCode
            """)
    boolean existsActiveByUserIdAndProductCode(
            @Param("userId") Long userId,
            @Param("productCode") String productCode
    );

    @Query("""
            select case when count(wc) > 0 then true else false end
            from WardrobeClothes wc
            join wc.clothes c
            join wc.wardrobe w
            join c.colorTags cc
            where w.user.id = :userId
              and wc.deletedAt is null
              and wc.ownershipStatus = :ownershipStatus
              and lower(c.brandName) = lower(:brandName)
              and lower(c.name) = lower(:name)
              and c.category = :category
              and c.itemType = :itemType
              and cc.colorRole = com.closetnangam.be.domain.clothes.enums.ColorRole.PRIMARY
              and cc.colorCode = :primaryColor
            """)
    boolean existsActiveByUserIdAndIdentity(
            @Param("userId") Long userId,
            @Param("brandName") String brandName,
            @Param("name") String name,
            @Param("category") String category,
            @Param("itemType") String itemType,
            @Param("primaryColor") String primaryColor,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );
    Optional<WardrobeClothes> findByWardrobe_User_IdAndClothes_Id(Long userId, Long clothesId);
}
