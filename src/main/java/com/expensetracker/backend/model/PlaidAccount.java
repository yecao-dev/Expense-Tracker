package com.expensetracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "plaid_accounts")
@Data
public class PlaidAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne means: "Many accounts can belong to one user"
    // @JoinColumn says: "Store the user's ID in a column called 'user_id'"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String accessToken;   // Plaid's permanent token — KEEP SECRET

    private String itemId;         // Plaid's identifier for this bank connection
    private String institutionName; // e.g., "Chase", "Bank of America"
    private String accountName;     // e.g., "Checking", "Savings"
    private String accountType;     // e.g., "depository", "credit"
}