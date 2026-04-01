package com.expensetracker.backend.repository;

import com.expensetracker.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

// JpaRepository<Transaction, Long> means:
// "This handles Transaction objects, and their IDs are of type Long"
// You get findAll(), findById(), save(), delete() for FREE.
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Spring auto-generates the SQL from the method name!
    // This becomes: SELECT * FROM transactions WHERE account_id IN (...)
    //               AND date BETWEEN ... AND ... ORDER BY date DESC
    List<Transaction> findByAccountIdInAndDateBetweenOrderByDateDesc(
        List<Long> accountIds, LocalDate start, LocalDate end
    );

    // Check if we already imported a transaction (prevent duplicates)
    boolean existsByPlaidTransactionId(String plaidTransactionId);

    // Custom query for spending by category
    @Query("SELECT t.aiCategory, SUM(t.amount) FROM Transaction t " +
           "WHERE t.account.user.id = :userId " +
           "AND t.date BETWEEN :start AND :end " +
           "GROUP BY t.aiCategory")
    List<Object[]> getSpendingByCategory(Long userId, LocalDate start, LocalDate end);
}