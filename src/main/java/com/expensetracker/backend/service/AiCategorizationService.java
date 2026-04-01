package com.expensetracker.backend.service;

import com.expensetracker.backend.model.CategoryResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiCategorizationService {

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CategoryResult categorize(String merchantName, double amount,
                                     String plaidCategory) {
        try {
            String systemPrompt = """
                You are a financial transaction categorizer. Given a transaction,
                assign it to exactly one category from this list:

                - Groceries
                - Dining Out
                - Coffee & Cafes
                - Transportation
                - Rideshare
                - Gas & Fuel
                - Rent & Housing
                - Utilities
                - Phone & Internet
                - Health & Pharmacy
                - Insurance
                - Subscriptions
                - Online Shopping
                - In-Store Shopping
                - Entertainment
                - Travel & Hotels
                - Education
                - Fitness & Gym
                - Personal Care
                - Gifts & Donations
                - Income
                - Transfer
                - Fees & Charges
                - Other

                Respond ONLY with a JSON object in this exact format:
                {"category": "...", "confidence": 0.XX, "reasoning": "..."}

                No markdown, no extra text. Just the JSON.
                """;

            String userPrompt = String.format(
                "Categorize this transaction:\n" +
                "Merchant: %s\n" +
                "Amount: $%.2f\n" +
                "Bank category hint: %s",
                merchantName, amount, plaidCategory
            );

            String requestBody = mapper.writeValueAsString(Map.of(
                "model", model,
                "max_tokens", 200,
                "system", systemPrompt,
                "messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
                )
            ));

            Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(requestBody,
                    MediaType.get("application/json")))
                .build();

            try (var response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                System.out.println("Claude API response: " + responseBody);
                JsonNode root = mapper.readTree(responseBody);
                String aiText = root.get("content").get(0).get("text").asText();
                JsonNode result = mapper.readTree(aiText);

                return new CategoryResult(
                    result.get("category").asText(),
                    result.get("confidence").asDouble(),
                    result.get("reasoning").asText()
                );
            }

        } catch (Exception e) {
            System.err.println("AI categorization failed: " + e.getMessage());
            return new CategoryResult(
                plaidCategory.isEmpty() ? "Other" : plaidCategory,
                0.5,
                "AI categorization failed, using Plaid default"
            );
        }
    }
}