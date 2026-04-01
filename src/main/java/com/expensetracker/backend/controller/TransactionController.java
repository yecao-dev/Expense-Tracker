package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.Transaction;
import com.expensetracker.backend.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionController {

    private final TransactionRepository transactionRepo;

    public TransactionController(TransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @RequestParam String start,
            @RequestParam String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Transaction> transactions = transactionRepo.findAll().stream()
            .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .collect(Collectors.toList());

        List<Map<String, Object>> result = transactions.stream()
            .map(t -> Map.<String, Object>of(
                "id", t.getId(),
                "date", t.getDate().toString(),
                "merchantName", t.getMerchantName() != null ? t.getMerchantName() : "Unknown",
                "aiCategory", t.getAiCategory() != null ? t.getAiCategory() : "Other",
                "aiConfidence", t.getAiConfidence() != null ? t.getAiConfidence() : 0.0,
                "amount", t.getAmount()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<Map<String, Object>>> getByCategory(
            @RequestParam String start,
            @RequestParam String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Transaction> transactions = transactionRepo.findAll().stream()
            .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
            .collect(Collectors.toList());

        Map<String, Double> categoryTotals = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getAiCategory() != null ? t.getAiCategory() : "Other",
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));

        List<Map<String, Object>> result = categoryTotals.entrySet().stream()
            .map(e -> Map.<String, Object>of(
                "category", e.getKey(),
                "total", e.getValue()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTrends(
            @RequestParam String start,
            @RequestParam String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Transaction> transactions = transactionRepo.findAll().stream()
            .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
            .collect(Collectors.toList());

        Map<String, Double> monthlyTotals = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getDate().getYear() + "-" +
                     String.format("%02d", t.getDate().getMonthValue()),
                Collectors.summingDouble(t -> Math.abs(t.getAmount().doubleValue()))
            ));

        List<Map<String, Object>> result = monthlyTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> Map.<String, Object>of(
                "month", e.getKey(),
                "total", e.getValue()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    public void exportCsv(
            @RequestParam String start,
            @RequestParam String end,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Transaction> transactions = transactionRepo.findAll().stream()
            .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .collect(Collectors.toList());

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");

        var writer = response.getWriter();
        writer.println("Date,Merchant,Category,Confidence,Amount");

        for (Transaction t : transactions) {
            writer.printf("%s,%s,%s,%.2f,%.2f%n",
                t.getDate(),
                t.getMerchantName() != null ? t.getMerchantName().replace(",", " ") : "Unknown",
                t.getAiCategory() != null ? t.getAiCategory() : "Other",
                t.getAiConfidence() != null ? t.getAiConfidence() : 0.0,
                t.getAmount());
        }

        writer.flush();
    }
}