package com.expensetracker.backend.repository;

import com.expensetracker.backend.model.PlaidAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaidAccountRepository extends JpaRepository<PlaidAccount, Long> {
    List<PlaidAccount> findByUserId(Long userId);
}