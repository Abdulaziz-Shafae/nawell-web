package com.example.capstone_3.Repository;

import com.example.capstone_3.Model.PendingSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 PendingSignup findByTokenHash(String tokenHash);
 PendingSignup findByEmailIgnoreCase(String email);
}
