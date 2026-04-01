package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.Budget;
import com.expensetracker.backend.model.Transaction;
import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.BudgetRepository;
import com.expensetracker.backend.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "http://localhost:3000")
public class BudgetController {

    private final BudgetRepository budgetRepo;
    private final TransactionRepository transactionRepo;

    public BudgetController(BudgetRepository budgetRepo,
                            TransactionRepository transactionRepo) {
        this.budgetRepo = budgetRepo;
        this.transactionRepo = transactionRepo;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createBudget(
            @RequestBody Map<String, String> body) {

        User user = (User) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(body.get("category"));
        budget.setMonthlyLimit(new BigDecimal(body.get("monthlyLimit")));
        budgetRepo.save(budget);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBudgets() {
        User user = (User) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

        List<Budget> budgets = budgetRepo.findByUserId(user.getId());

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        List<Transaction> monthTransactions = transactionRepo.findAll().stream()
            .filter(t -> !t.getDate().isBefore(monthStart) && !t.getDate().isAfter(now))
            .collect(Collectors.toList());

        Map<String, Double> spentByCategory = monthTransactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getAiCategory() != null ? t.getAiCategory() : "Other",
                Collectors.summingDouble(t -> Math.abs(t.getAmount().doubleValue()))
            ));

        List<Map<String, Object>> result = budgets.stream()
            .map(b -> {
                double spent = spentByCategory.getOrDefault(b.getCategory(), 0.0);
                double limit = b.getMonthlyLimit().doubleValue();
                double percentage = limit > 0 ? (spent / limit) * 100 : 0;

                return Map.<String, Object>of(
                    "id", b.getId(),
                    "category", b.getCategory(),
                    "monthlyLimit", limit,
                    "spent", spent,
                    "percentage", percentage,
                    "overBudget", spent > limit
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBudget(@PathVariable Long id) {
        budgetRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}