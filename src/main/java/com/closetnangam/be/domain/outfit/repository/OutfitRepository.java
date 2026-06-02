package com.closetnangam.be.domain.outfit.repository;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    @Query("""
            select o
            from Outfit o
            join fetch o.outfitBook ob
            where ob.id = :bookId
            order by o.createdAt desc
            """)
    List<Outfit> findAllByOutfitBookId(@Param("bookId") Long bookId);
}
