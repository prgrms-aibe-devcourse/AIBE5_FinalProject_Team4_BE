package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WardrobeClothesRepository extends JpaRepository<WardrobeClothes, Long> {

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            left join fetch c.colorTags
            left join fetch c.styleTags st
            left join fetch st.style
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and c.sourceType = :sourceType
            order by c.createdAt desc
            """)
    List<WardrobeClothes> findAllByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") SourceType sourceType
    );

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            left join fetch c.colorTags
            left join fetch c.styleTags st
            left join fetch st.style
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and c.sourceType = :sourceType
              and wc.favorite = true
            order by wc.updatedAt desc
            """)
    List<WardrobeClothes> findFavoritesByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") SourceType sourceType
    );

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            left join fetch c.colorTags
            left join fetch c.styleTags st
            left join fetch st.style
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id = :clothesId
            """)
    Optional<WardrobeClothes> findByClothesIdWithDetails(@Param("clothesId") Long clothesId);

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            where c.id = :clothesId and w.user.id = :userId
            """)
    Optional<WardrobeClothes> findByClothesIdAndUserId(
            @Param("clothesId") Long clothesId,
            @Param("userId") Long userId
    );

    void deleteByClothes_Id(Long clothesId);
}
