package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    Optional<Clothes> findByExternalProductId(String externalProductId);

    @Query("SELECT c FROM Clothes c WHERE c.infoSource IN ('EXTERNAL_SHOPPING', 'PURCHASE_HISTORY') ORDER BY c.createdAt DESC")
    List<Clothes> findAllForRecommendation(Pageable pageable);

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
