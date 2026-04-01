package com.expensetracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private PlaidAccount account;

    @Column(unique = true)
    private String plaidTransactionId; // Plaid's unique ID — prevents duplicates

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    // ↑ Always use BigDecimal for money! "double" has floating-point errors
    //   e.g., 0.1 + 0.2 = 0.30000000000000004 with double

    @Column(nullable = false)
    private LocalDate date;

    private String merchantName;       // "Starbucks", "Amazon", etc.
    private String originalCategory;   // What Plaid says: "Food and Drink"
    private String aiCategory;         // What Claude says: "Coffee & Cafes"
    private Double aiConfidence;       // How sure Claude is: 0.95 = 95%
}