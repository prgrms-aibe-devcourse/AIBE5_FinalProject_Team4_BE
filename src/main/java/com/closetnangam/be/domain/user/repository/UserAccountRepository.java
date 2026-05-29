package com.closetnangam.be.domain.user.repository;

import com.closetnangam.be.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByProviderAndProviderId(String provider, String providerId);
}
