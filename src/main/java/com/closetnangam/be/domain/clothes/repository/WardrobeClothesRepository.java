package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WardrobeClothesRepository extends JpaRepository<WardrobeClothes, Long> {

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.wardrobe w
            join fetch w.user
            join fetch wc.clothes c
            left join fetch c.styleTags st
            left join fetch st.style
            where wc.id = :wardrobeClothesId
            """)
    Optional<WardrobeClothes> findByIdWithDetails(@Param("wardrobeClothesId") Long wardrobeClothesId);
}
