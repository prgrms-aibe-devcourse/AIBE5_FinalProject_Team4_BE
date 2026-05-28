package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

    /**
     * ClothesResponse 매핑을 위해 wardrobe, user, styleTags, style을 join fetch합니다.
     * fetch 대상 변경 시 {@link com.closetnangam.be.domain.clothes.dto.response.ClothesResponse#from}도 함께 검토하세요.
     */
    @Query("""
            select distinct c from Clothes c
            join fetch c.wardrobe w
            join fetch w.user
            left join fetch c.styleTags st
            left join fetch st.style
            where w.user.id = :userId and c.sourceType = :sourceType
            order by c.createdAt desc
            """)
    List<Clothes> findAllByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") SourceType sourceType
    );

    @Query("""
            select c from Clothes c
            join fetch c.wardrobe w
            join fetch w.user
            left join fetch c.styleTags st
            left join fetch st.style
            where c.id = :clothesId
            """)
    Optional<Clothes> findByIdWithDetails(@Param("clothesId") Long clothesId);

    @Query("""
            select distinct c from Clothes c
            join fetch c.wardrobe w
            join fetch w.user
            left join fetch c.styleTags st
            left join fetch st.style
            where w.user.id = :userId
              and c.sourceType = :sourceType
              and c.isFavorite = true
            order by c.updatedAt desc
            """)
    List<Clothes> findFavoritesByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") SourceType sourceType
    );
}
