package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    Optional<Clothes> findByExternalProductId(String externalProductId);

    long countByClothesInfoSource(ClothesInfoSource clothesInfoSource);

    @Query("SELECT c FROM Clothes c WHERE c.clothesInfoSource IN ('EXTERNAL_SHOPPING', 'PURCHASE_HISTORY') ORDER BY c.createdAt DESC")
    List<Clothes> findAllForRecommendation(Pageable pageable);

    /**
     * RECO-004 어울리는 옷 추천 전용 외부 후보 조회.
     * RECO-002 취향 기반 추천과 분리해 EXTERNAL_SHOPPING만 사용한다.
     */
    @Query("""
            select distinct c from Clothes c
            left join fetch c.styleTags st
            left join fetch st.style
            where c.clothesInfoSource = com.closetnangam.be.domain.clothes.enums.ClothesInfoSource.EXTERNAL_SHOPPING
              and c.category <> :excludeCategory
              and c.category in ('TOP', 'BOTTOM', 'OUTER', 'SHOES')
            order by c.createdAt desc
            """)
    List<Clothes> findExternalCandidatesForComplementaryRecommendation(
            @Param("excludeCategory") String excludeCategory,
            Pageable pageable
    );

    @Query("""
    SELECT s.name, COUNT(cst.id) as styleCount
    FROM WardrobeClothes wc
    JOIN wc.clothes c
    JOIN c.styleTags cst
    JOIN cst.style s
    WHERE wc.wardrobe.id = :wardrobeId
    GROUP BY s.name
    ORDER BY styleCount DESC
    """)
    List<Object[]> countStyleTagsByWardrobe(@Param("wardrobeId") Long wardrobeId, Pageable pageable);
}
