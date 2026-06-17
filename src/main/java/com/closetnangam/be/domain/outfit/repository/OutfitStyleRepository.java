package com.closetnangam.be.domain.outfit.repository;


import com.closetnangam.be.domain.outfit.entity.OutfitStyles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutfitStyleRepository extends JpaRepository<OutfitStyles, Long> {

    List<OutfitStyles> findAllByOutfit_OutfitId(Long outfitId);

    @Modifying
    @Query("DELETE FROM OutfitStyles os WHERE os.outfit.outfitId = :outfitId")
    void deleteAllByOutfit_OutfitId(@Param("outfitId") Long outfitId);
}