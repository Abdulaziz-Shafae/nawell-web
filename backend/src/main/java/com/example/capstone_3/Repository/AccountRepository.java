package com.example.capstone_3.Repository;

import com.example.capstone_3.Model.Account;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    Account findAccountById(Integer id);

    Account findAccountByEmail(String email);

    @Query(value = "SELECT * FROM account WHERE id = :id FOR UPDATE", nativeQuery = true)
    Account findAccountForVerification(@Param("id") Integer id);

    @Query(value = "SELECT * FROM account WHERE id = :id FOR UPDATE", nativeQuery = true)
    Account findAccountForTokenUpdate(@Param("id") Integer id);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE account SET token_balance = token_balance - :amount WHERE id = :accountId AND token_balance >= :amount AND status = 'ACTIVE' AND email_verified = true", nativeQuery = true)
    int deductTokens(@Param("accountId") Integer accountId, @Param("amount") Integer amount);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE account SET token_balance = token_balance + :amount WHERE id = :accountId AND token_balance IS NOT NULL", nativeQuery = true)
    int refundTokens(@Param("accountId") Integer accountId, @Param("amount") Integer amount);

}