package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.PlaidAccount;
import com.expensetracker.backend.model.Transaction;
import com.expensetracker.backend.repository.PlaidAccountRepository;
import com.expensetracker.backend.repository.TransactionRepository;
import com.expensetracker.backend.service.PlaidService;
import com.expensetracker.backend.service.AiCategorizationService;
import com.expensetracker.backend.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController             // Combines @Controller + @ResponseBody (returns JSON)
@RequestMapping("/api/plaid")
@CrossOrigin(origins = "http://localhost:3000")  // Allow React to call this
public class PlaidController {

private final PlaidService plaidService;
    private final PlaidAccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final AiCategorizationService aiService;

    public PlaidController(PlaidService plaidService,
                           PlaidAccountRepository accountRepo,
                           TransactionRepository transactionRepo,
                           AiCategorizationService aiService) {
        this.plaidService = plaidService;
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
        this.aiService = aiService;
    }

    /**
     * Frontend calls this first to get a link_token for Plaid Link.
     * POST /api/plaid/create-link-token
     */
    @PostMapping("/create-link-token")
    public ResponseEntity<Map<String, String>> createLinkToken() throws Exception {
        // In a real app, get userId from the authenticated session
        String linkToken = plaidService.createLinkToken("user-1");
        return ResponseEntity.ok(Map.of("link_token", linkToken));
    }

    /**
     * Frontend calls this after user completes Plaid Link.
     * POST /api/plaid/exchange-token
     * Body: { "public_token": "public-sandbox-..." }
     */
    @PostMapping("/exchange-token")
    public ResponseEntity<Map<String, String>> exchangeToken(
            @RequestBody Map<String, String> body) throws Exception {

        String publicToken = body.get("public_token");
        var exchangeResponse = plaidService.exchangePublicToken(publicToken);

        // Get the authenticated user from Spring Security
        User user = (User) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

        PlaidAccount account = new PlaidAccount();
        account.setUser(user);
        account.setAccessToken(exchangeResponse.getAccessToken());
        account.setItemId(exchangeResponse.getItemId());
        accountRepo.save(account);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Fetch + categorize + save transactions.
     * POST /api/plaid/sync-transactions
     *
     * This is where the magic happens:
     * 1. Pull transactions from Plaid
     * 2. Send each to Claude AI for smart categorization
     * 3. Save to our database
     */
    @PostMapping("/sync-transactions")
    public ResponseEntity<Map<String, Object>> syncTransactions() throws Exception {
        // In real app: get accounts for the authenticated user
        List<PlaidAccount> accounts = accountRepo.findAll();
        int newCount = 0;

        for (PlaidAccount account : accounts) {
            // Fetch last 30 days of transactions
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);

            var plaidTransactions = plaidService.getTransactions(
                account.getAccessToken(), start, end);

            for (var pt : plaidTransactions) {
                // Skip if we already have this transaction
                if (transactionRepo.existsByPlaidTransactionId(pt.getTransactionId())) {
                    continue;
                }

                // Ask Claude AI to categorize it
                var categoryResult = aiService.categorize(
                    pt.getName(),                          // merchant name
                    pt.getAmount(),                        // dollar amount
                    pt.getCategory() != null ?
                        String.join(", ", pt.getCategory()) : ""  // Plaid's rough category
                );

                // Build our Transaction entity
                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setPlaidTransactionId(pt.getTransactionId());
                tx.setAmount(BigDecimal.valueOf(pt.getAmount()));
                tx.setDate(pt.getDate());
                tx.setMerchantName(pt.getName());
                tx.setOriginalCategory(
                    pt.getCategory() != null ?
                        String.join(", ", pt.getCategory()) : "Unknown"
                );
                tx.setAiCategory(categoryResult.getCategory());
                tx.setAiConfidence(categoryResult.getConfidence());

                transactionRepo.save(tx);
                newCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "new_transactions", newCount
        ));
    }
}