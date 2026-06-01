package com.closetnangam.be.domain.purchase.repository;

import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseCaptureRepository extends JpaRepository<PurchaseCapture, Long> {

    Optional<PurchaseCapture> findByIdAndUser_Id(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PurchaseCapture p WHERE p.id = :id AND p.user.id = :userId")
    Optional<PurchaseCapture> findByIdAndUser_IdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
