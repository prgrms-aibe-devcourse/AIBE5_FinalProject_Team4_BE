package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {
}
