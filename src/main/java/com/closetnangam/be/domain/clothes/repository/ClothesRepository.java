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
     * RECO-004 어울리는 옷 추천 후보 — {@link ClothesInfoSource#EXTERNAL_SHOPPING} 공용 마스터만 카테고리별 최신순 조회.
     *
     * <p>PHOTO·PURCHASE_HISTORY 등 사용자 개인 등록 마스터는 후보에서 제외합니다.
     * styleTags/colorTags는 {@link Clothes}의 {@code @Fetch(SUBSELECT)}로 별도 로딩합니다.
     * fetch join + DISTINCT는 Pageable LIMIT이 DB가 아닌 메모리에서 적용되는 HHH90003004를 유발할 수 있어 사용하지 않습니다.
     */
    @Query("""
            select c from Clothes c
            where c.clothesInfoSource = com.closetnangam.be.domain.clothes.enums.ClothesInfoSource.EXTERNAL_SHOPPING
              and c.category = :category
            order by c.createdAt desc
            """)
    List<Clothes> findComplementaryRecommendationCandidatesByCategory(
            @Param("category") String category,
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
