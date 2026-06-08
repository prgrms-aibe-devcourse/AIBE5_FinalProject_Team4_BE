package com.closetnangam.be.domain.outfit.repository;

import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
}
