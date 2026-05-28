package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

    @Query("""
            select c from Clothes c
            left join fetch c.colorTags
            left join fetch c.styleTags st
            left join fetch st.style
            where c.id = :clothesId
            """)
    Optional<Clothes> findByIdWithDetails(@Param("clothesId") Long clothesId);
}
