# Building an AI-Powered Expense Tracker — Step-by-Step Guide

A hands-on reference for building a full-stack expense tracking application from scratch using Spring Boot, React, PostgreSQL, Plaid API, and Claude AI. This guide assumes a Java background with no prior project experience and walks through every milestone with logic explanations, code snippets, and tool choices.

---

## Architecture Overview

Before writing any code, understand the three-layer architecture you're building:

```
┌──────────────────────┐       ┌──────────────────────────┐       ┌──────────────┐
│  React Frontend      │◄─────►│  Spring Boot REST API    │◄─────►│  PostgreSQL  │
│  (Vercel)            │ REST  │  (Railway)               │       │  (Railway)   │
│                      │ JSON  │                          │       │              │
│  - Login page        │       │  - AuthController        │       │  4 Tables:   │
│  - Dashboard         │       │  - PlaidController       │       │  - users     │
│  - Transaction list  │       │  - TransactionController │       │  - plaid_acct│
│  - Charts            │       │  - BudgetController      │       │  - txns      │
│  - Budget alerts     │       │  - JWT security filter   │       │  - budgets   │
└──────────────────────┘       └──────┬───────┬───────────┘       └──────────────┘
                                      │       │
                                      ▼       ▼
                               ┌──────────┐ ┌──────────┐
                               │ Plaid API│ │Claude API│
                               │ (Banking)│ │ (AI Cat.)│
                               └──────────┘ └──────────┘
```

The frontend talks to the backend via REST/JSON. The backend talks to two external APIs (Plaid for bank data, Claude for AI categorization) and persists everything in PostgreSQL. JWT tokens secure every request after login.

---

## Milestone 1: Project Setup

### 1.1 Why These Tools?

| Decision           | Choice                   | Why                                                          |
| ------------------ | ------------------------ | ------------------------------------------------------------ |
| Backend framework  | Spring Boot              | Industry standard for Java REST APIs; auto-configuration saves boilerplate; massive ecosystem |
| Frontend framework | React (not React Native) | React is for web apps; React Native is for mobile. This project is web-first |
| Database           | PostgreSQL               | Production-grade relational DB; free tier on Railway; strong JSON and date support |
| Build tool         | Maven                    | Standard for Spring Boot; manages dependencies via `pom.xml` |
| Java version       | Java 21 (LTS)            | Latest long-term support release; required by Spring Boot 4.x |

> **Common confusion:** React vs React Native. React builds web apps that run in browsers. React Native builds native mobile apps for iOS/Android. For a portfolio project targeting SDE roles, React (web) is the right choice — it's what most companies use for internal tools and dashboards.

### 1.2 Generate the Spring Boot Project

Go to [start.spring.io](https://start.spring.io) and configure:

- **Project:** Maven
- **Language:** Java
- **Spring Boot:** 4.0.3
- **Group:** `com.expensetracker`
- **Artifact:** `backend`
- **Packaging:** Jar
- **Java:** 21

Add these dependencies (you can search for them in the UI):

- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Lombok

Click **Generate**, download, and unzip. This gives you a working skeleton with `BackendApplication.java` as the entry point.

### 1.3 Add Non-Starter Dependencies to pom.xml

Spring Initializr doesn't include everything. Add these manually inside `<dependencies>`:

```xml
<!-- Plaid Java SDK — connects to bank accounts -->
<dependency>
    <groupId>com.plaid</groupId>
    <artifactId>plaid-java</artifactId>
    <version>38.2.0</version>
</dependency>

<!-- OkHttp — HTTP client for calling Claude API -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JWT libraries — token generation and validation -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Why OkHttp instead of the Anthropic Java SDK?** At the time of this project, OkHttp gave more direct control over the HTTP request to Claude's `/v1/messages` endpoint. You construct the JSON payload yourself, which helps you understand what's actually being sent. If Anthropic releases an official Java SDK, that would be a cleaner alternative.

### 1.4 Configure application.yml

Replace the auto-generated `application.properties` with `application.yml` (YAML is more readable for nested config). Create `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/expense_tracker}
    username: ${SPRING_DATASOURCE_USERNAME:expense_user}
    password: ${SPRING_DATASOURCE_PASSWORD:your_secure_password}
  jpa:
    hibernate:
      ddl-auto: update    # Auto-creates/updates tables from entities
    show-sql: true        # Logs SQL queries — helpful for debugging

plaid:
  client-id: ${PLAID_CLIENT_ID:your_client_id_here}
  secret: ${PLAID_SECRET:your_secret_here}
  env: ${PLAID_ENV:sandbox}

anthropic:
  api-key: ${ANTHROPIC_API_KEY:your_api_key_here}
  model: ${ANTHROPIC_MODEL:claude-sonnet-4-20250514}

jwt:
  secret: ${JWT_SECRET:change-me-in-production}
  expiration: ${JWT_EXPIRATION:86400000}   # 24 hours in milliseconds
```

**Key concept: `${ENV_VAR:default}` syntax.** This reads from environment variables at runtime. The value after `:` is a fallback for local development. Real secrets go in a `.env` file (gitignored) or in your hosting platform's environment variable settings. Never commit real API keys.

### 1.5 Create the Local Database

```bash
# Install PostgreSQL (macOS)
brew install postgresql@16
brew services start postgresql@16

# Create the database and user
psql postgres
CREATE USER expense_user WITH PASSWORD 'your_secure_password';
CREATE DATABASE expense_tracker OWNER expense_user;
GRANT ALL PRIVILEGES ON DATABASE expense_tracker TO expense_user;
\q
```

### 1.6 Create the .env File (Gitignored)

Create `.env` in your project root:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/expense_tracker
SPRING_DATASOURCE_USERNAME=expense_user
SPRING_DATASOURCE_PASSWORD=your_secure_password
PLAID_CLIENT_ID=your_plaid_client_id
PLAID_SECRET=your_plaid_sandbox_secret
PLAID_ENV=sandbox
ANTHROPIC_API_KEY=sk-ant-api03-...
ANTHROPIC_MODEL=claude-sonnet-4-20250514
JWT_SECRET=a-long-random-string-at-least-32-chars
JWT_EXPIRATION=86400000
```

Add `.env` to `.gitignore` immediately.

### 1.7 Verify the Setup

```bash
./mvnw spring-boot:run
```

If it starts and connects to PostgreSQL without errors, Milestone 1 is complete. You should see Hibernate auto-creating tables once you add entities in Milestone 2.

### 1.8 Initialize the React Frontend

```bash
npx create-react-app frontend
cd frontend
npm install axios react-router-dom recharts react-plaid-link
```

| Package            | What it does                                             |
| ------------------ | -------------------------------------------------------- |
| `axios`            | HTTP client for calling your Spring Boot API             |
| `react-router-dom` | Client-side page routing (login page vs dashboard)       |
| `recharts`         | Charting library for pie charts, line charts, bar charts |
| `react-plaid-link` | Official Plaid widget that opens the bank-connection UI  |

---

## Milestone 2: Database Design

### 2.1 Entity-Relationship Thinking

Before writing code, think about the data relationships:

- A **User** has many **PlaidAccounts** (one per connected bank)
- A **PlaidAccount** has many **Transactions**
- A **User** has many **Budgets** (one per spending category)

This gives you four JPA entities with `@ManyToOne` relationships forming a clean hierarchy: `User → PlaidAccount → Transaction` and `User → Budget`.

### 2.2 The User Entity

```java
package com.expensetracker.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")   // "user" is a reserved word in PostgreSQL
@Data                      // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String passwordHash;   // Never store plaintext passwords

    private LocalDateTime createdAt;

    @PrePersist   // JPA lifecycle callback — runs before first save
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

**Why `@Table(name = "users")`?** PostgreSQL reserves the word `user`, so the table must be named `users`. Without this annotation, Hibernate would try to create a table called `user` and fail.

**Why Lombok's `@Data`?** It auto-generates getters, setters, `toString()`, `equals()`, and `hashCode()`. This eliminates hundreds of lines of boilerplate. The `@Builder` annotation lets you construct objects with a fluent API: `User.builder().email("a@b.com").name("Test").build()`.

### 2.3 The PlaidAccount Entity

```java
@Entity
@Table(name = "plaid_accounts")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaidAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)   // Don't load User unless accessed
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String accessToken;    // Plaid's permanent token for this bank connection

    private String itemId;          // Plaid's identifier for the connection
    private String institutionName; // e.g., "Chase", "Wells Fargo"
    private String accountName;     // e.g., "Checking", "Savings"
    private String accountType;     // e.g., "depository", "credit"
}
```

**Why `FetchType.LAZY`?** By default, `@ManyToOne` uses `EAGER` fetching, meaning every time you load a PlaidAccount, it also loads the User. With `LAZY`, the User is only loaded when you actually call `getUser()`. This matters for performance when you're loading hundreds of transactions — you don't need the User object for each one.

### 2.4 The Transaction Entity

```java
@Entity
@Table(name = "transactions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private PlaidAccount account;

    @Column(unique = true)
    private String plaidTransactionId;   // Prevents duplicate imports

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDate date;
    private String merchantName;
    private String originalCategory;     // What Plaid returns (coarse)
    private String aiCategory;           // What Claude AI returns (fine-grained)
    private Double aiConfidence;         // 0.0 to 1.0
}
```

**Why `plaidTransactionId` with `unique = true`?** When you sync transactions, Plaid may return transactions you've already imported. The unique constraint on Plaid's transaction ID means the database will reject duplicates automatically — you catch the `DataIntegrityViolationException` and skip it.

**Why `BigDecimal` instead of `double`?** Floating-point types (`double`, `float`) can't represent money precisely. `0.1 + 0.2` equals `0.30000000000000004` in double math. `BigDecimal` stores exact decimal values. Always use it for currency.

### 2.5 The Budget Entity

```java
@Entity
@Table(name = "budgets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category;       // Must match one of the 24 AI categories

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyLimit;
}
```

### 2.6 Repository Layer

Spring Data JPA generates the SQL for you. You just declare interfaces:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

public interface PlaidAccountRepository extends JpaRepository<PlaidAccount, Long> {
    List<PlaidAccount> findByUserId(Long userId);
}

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
}
```

For transactions, you need custom JPQL queries for analytics:

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions for a user, ordered by date
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId " +
           "ORDER BY t.date DESC")
    List<Transaction> findByUserId(@Param("userId") Long userId);

    // Spending grouped by AI category — powers the pie chart
    @Query("SELECT t.aiCategory, SUM(t.amount) FROM Transaction t " +
           "WHERE t.account.user.id = :userId " +
           "GROUP BY t.aiCategory ORDER BY SUM(t.amount) DESC")
    List<Object[]> findSpendingByCategory(@Param("userId") Long userId);

    // Monthly totals — powers the trend line chart
    @Query("SELECT FUNCTION('TO_CHAR', t.date, 'YYYY-MM'), SUM(t.amount) " +
           "FROM Transaction t WHERE t.account.user.id = :userId " +
           "GROUP BY FUNCTION('TO_CHAR', t.date, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', t.date, 'YYYY-MM')")
    List<Object[]> findMonthlyTrends(@Param("userId") Long userId);

    boolean existsByPlaidTransactionId(String plaidTransactionId);
}
```

**Why `Object[]` return types?** When you `SELECT` multiple columns with `GROUP BY`, JPA returns each row as an `Object[]` array. Index 0 is the category name, index 1 is the sum. You map these to DTOs in the controller.

Run the backend again. Hibernate will auto-create all four tables:

```bash
./mvnw spring-boot:run
# Check: psql expense_tracker -c "\dt"
# You should see: users, plaid_accounts, transactions, budgets
```

---

## Milestone 3: Plaid Integration

### 3.1 How Plaid Works (The Mental Model)

Plaid is a middleman between your app and thousands of banks. The flow has three steps:

```
Step 1: Your backend asks Plaid for a "link token"
Step 2: Your frontend opens Plaid Link (a secure widget) using that token
        → User logs into their bank inside the widget
        → Plaid returns a temporary "public token" to your frontend
Step 3: Your backend exchanges the public token for a permanent "access token"
        → You store the access token and use it to fetch transactions anytime
```

This is called the **token exchange pattern** and it's used by many financial APIs. The user's bank credentials never touch your server — Plaid handles all of that.

### 3.2 Get Plaid API Credentials

1. Sign up at [dashboard.plaid.com](https://dashboard.plaid.com)
2. Get your `client_id` and sandbox `secret` from the dashboard
3. Add them to your `.env` file

Sandbox mode returns fake bank data, which is perfect for development. You won't need real bank credentials.

### 3.3 Configure the Plaid Client Bean

```java
package com.expensetracker.backend.config;

import com.plaid.client.ApiClient;
import com.plaid.client.request.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaidConfig {

    @Value("${plaid.client-id}")
    private String clientId;

    @Value("${plaid.secret}")
    private String secret;

    @Value("${plaid.env}")
    private String env;

    @Bean
    public ApiClient plaidApiClient() {
        // HashMap maps "sandbox" → Plaid's sandbox URL, etc.
        ApiClient apiClient = new ApiClient(
            env.equals("sandbox") ? ApiClient.Sandbox : ApiClient.Production
        );
        apiClient.setPlaidCredentials(clientId, secret);
        return apiClient;
    }
}
```

**Why a `@Bean`?** Spring's dependency injection lets you create the Plaid client once and inject it anywhere. Without this, you'd be creating a new client in every service method.

### 3.4 The Plaid Service

```java
@Service
public class PlaidService {

    private final PlaidApi plaidApi;

    public PlaidService(ApiClient apiClient) {
        this.plaidApi = apiClient.createService(PlaidApi.class);
    }

    // Step 1: Generate a link token for the frontend
    public String createLinkToken(Long userId) throws Exception {
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .user(new LinkTokenCreateRequestUser().clientUserId(userId.toString()))
            .clientName("Expense Tracker")
            .products(List.of(Products.TRANSACTIONS))
            .countryCodes(List.of(CountryCode.US))
            .language("en");

        LinkTokenCreateResponse response = plaidApi
            .linkTokenCreate(request)
            .execute()
            .body();

        return response.getLinkToken();
    }

    // Step 3: Exchange temporary public token for permanent access token
    public ItemPublicTokenExchangeResponse exchangePublicToken(String publicToken)
            throws Exception {
        ItemPublicTokenExchangeRequest request =
            new ItemPublicTokenExchangeRequest().publicToken(publicToken);

        return plaidApi
            .itemPublicTokenExchange(request)
            .execute()
            .body();
    }

    // Fetch transactions using the stored access token
    public TransactionsGetResponse getTransactions(
            String accessToken, LocalDate start, LocalDate end) throws Exception {
        TransactionsGetRequest request = new TransactionsGetRequest()
            .accessToken(accessToken)
            .startDate(start)
            .endDate(end);

        return plaidApi
            .transactionsGet(request)
            .execute()
            .body();
    }
}
```

### 3.5 The Plaid Controller

```java
@RestController
@RequestMapping("/api/plaid")
public class PlaidController {

    @Autowired private PlaidService plaidService;
    @Autowired private PlaidAccountRepository plaidAccountRepo;
    @Autowired private TransactionRepository transactionRepo;
    @Autowired private AiCategorizationService aiService;

    @PostMapping("/create-link-token")
    public ResponseEntity<?> createLinkToken(Authentication auth) {
        // auth.getName() returns the user ID from the JWT
        Long userId = Long.parseLong(auth.getName());
        String linkToken = plaidService.createLinkToken(userId);
        return ResponseEntity.ok(Map.of("link_token", linkToken));
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<?> exchangeToken(
            @RequestBody Map<String, String> body, Authentication auth) {
        String publicToken = body.get("public_token");
        Long userId = Long.parseLong(auth.getName());

        var response = plaidService.exchangePublicToken(publicToken);

        // Save the permanent access token
        PlaidAccount account = PlaidAccount.builder()
            .user(User.builder().id(userId).build())
            .accessToken(response.getAccessToken())
            .itemId(response.getItemId())
            .build();
        plaidAccountRepo.save(account);

        return ResponseEntity.ok(Map.of("status", "connected"));
    }

    @PostMapping("/sync-transactions")
    public ResponseEntity<?> syncTransactions(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<PlaidAccount> accounts = plaidAccountRepo.findByUserId(userId);

        int synced = 0;
        for (PlaidAccount account : accounts) {
            var response = plaidService.getTransactions(
                account.getAccessToken(),
                LocalDate.now().minusDays(30),
                LocalDate.now()
            );

            for (var txn : response.getTransactions()) {
                // Skip if already imported
                if (transactionRepo.existsByPlaidTransactionId(txn.getTransactionId())) {
                    continue;
                }

                // AI categorization
                var aiResult = aiService.categorize(
                    txn.getName(),
                    txn.getAmount(),
                    txn.getCategory() != null ? txn.getCategory().toString() : ""
                );

                Transaction transaction = Transaction.builder()
                    .account(account)
                    .plaidTransactionId(txn.getTransactionId())
                    .amount(BigDecimal.valueOf(txn.getAmount()))
                    .date(txn.getDate())
                    .merchantName(txn.getName())
                    .originalCategory(txn.getCategory() != null ?
                        txn.getCategory().toString() : "Unknown")
                    .aiCategory(aiResult.getCategory())
                    .aiConfidence(aiResult.getConfidence())
                    .build();

                transactionRepo.save(transaction);
                synced++;
            }
        }
        return ResponseEntity.ok(Map.of("synced", synced));
    }
}
```

**The sync flow explained:** For each connected bank account, fetch the last 30 days of transactions from Plaid. For each transaction, check if it already exists (by Plaid's ID). If it's new, send it to Claude AI for categorization, then save it. This is the core pipeline of the entire app.

### 3.6 Frontend: Plaid Link Component

```jsx
// PlaidLink.jsx
import { usePlaidLink } from 'react-plaid-link';
import api from '../services/api';
import { useState, useEffect } from 'react';

function PlaidLinkButton({ onSuccess }) {
    const [linkToken, setLinkToken] = useState(null);

    useEffect(() => {
        // Step 1: Get link token from your backend
        api.post('/plaid/create-link-token')
           .then(res => setLinkToken(res.data.link_token));
    }, []);

    const { open, ready } = usePlaidLink({
        token: linkToken,
        onSuccess: async (publicToken) => {
            // Step 2 complete — Plaid gives us the public token
            // Step 3: Send it to your backend to exchange
            await api.post('/plaid/exchange-token', {
                public_token: publicToken
            });
            onSuccess();   // Refresh the dashboard
        },
    });

    return (
        <button onClick={() => open()} disabled={!ready}>
            Connect Your Bank
        </button>
    );
}
```

---

## Milestone 4: AI Categorization Logic

### 4.1 Why Use an LLM for Categorization?

Plaid returns broad categories like "Food and Drink" or "Travel." But users want to know: was it coffee, dining out, or groceries? An LLM can read the merchant name (e.g., "STARBUCKS #4523") and infer a much more specific category with high confidence.

You could also use rule-based matching (if merchant contains "Starbucks" → Coffee), but that breaks on new or unusual merchants. The LLM handles edge cases gracefully.

### 4.2 The CategoryResult POJO

```java
package com.expensetracker.backend.model;

import lombok.Data;

@Data
public class CategoryResult {
    private String category;
    private double confidence;
    private String reasoning;
}
```

### 4.3 The AI Categorization Service

```java
@Service
public class AiCategorizationService {

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        You are a financial transaction categorizer. Given a transaction's merchant name,
        amount, and rough category, classify it into exactly ONE of these categories:

        Groceries, Dining Out, Coffee & Cafes, Transportation, Rideshare, Gas & Fuel,
        Rent & Housing, Utilities, Phone & Internet, Health & Pharmacy, Insurance,
        Subscriptions, Online Shopping, In-Store Shopping, Entertainment,
        Travel & Hotels, Education, Fitness & Gym, Personal Care,
        Gifts & Donations, Income, Transfer, Fees & Charges, Other

        Respond ONLY with valid JSON in this format:
        {"category": "...", "confidence": 0.95, "reasoning": "..."}

        confidence should be 0.0 to 1.0 indicating how certain you are.
        """;

    public CategoryResult categorize(
            String merchantName, double amount, String plaidCategory) {
        try {
            String userPrompt = String.format(
                "Merchant: %s | Amount: $%.2f | Plaid category hint: %s",
                merchantName, amount, plaidCategory
            );

            // Build the JSON request body manually
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "max_tokens", 200,
                "system", SYSTEM_PROMPT,
                "messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
                )
            ));

            Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(RequestBody.create(jsonBody,
                    MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();

                // Parse Claude's response — extract the text content
                JsonNode root = objectMapper.readTree(responseBody);
                String text = root.get("content").get(0).get("text").asText();

                // Claude returns JSON inside its text response
                // Strip any markdown fences if present
                String cleanJson = text
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

                return objectMapper.readValue(cleanJson, CategoryResult.class);
            }

        } catch (Exception e) {
            // Fallback: use Plaid's category with low confidence
            CategoryResult fallback = new CategoryResult();
            fallback.setCategory(plaidCategory.isEmpty() ? "Other" : plaidCategory);
            fallback.setConfidence(0.5);
            fallback.setReasoning("AI categorization failed, using Plaid default");
            return fallback;
        }
    }
}
```

### 4.4 Understanding the Code Step by Step

**The system prompt** tells Claude exactly what to do and constrains its output format. Key design decisions:

1. List all 24 valid categories explicitly so Claude doesn't invent new ones.
2. Request JSON output with a specific schema. This makes parsing deterministic.
3. Include a confidence score so the frontend can flag uncertain categorizations.

**The user prompt** gives Claude three signals per transaction: the merchant name (most informative), the dollar amount (helps distinguish e.g., a $3 coffee from a $50 grocery run at the same store), and Plaid's rough category as a hint.

**The fallback** is critical. API calls fail for many reasons — rate limits, network issues, invalid keys. The `catch` block ensures the app never crashes; it just falls back to Plaid's less-specific category with 0.5 confidence.

**Why `max_tokens: 200`?** The JSON response is short (~50 tokens). Setting 200 gives plenty of room while keeping costs low. Each transaction costs roughly $0.001 to categorize.

### 4.5 Example Results

| Merchant            | Plaid Says     | Claude AI Says  | Confidence |
| ------------------- | -------------- | --------------- | ---------- |
| STARBUCKS #4523     | Food and Drink | Coffee & Cafes  | 0.97       |
| United Airlines     | Travel         | Travel & Hotels | 0.95       |
| Uber 072515 SF      | Travel         | Rideshare       | 0.95       |
| KFC                 | Food and Drink | Dining Out      | 0.95       |
| Touchstone Climbing | Recreation     | Fitness & Gym   | 0.95       |

Notice how Claude disambiguates "Food and Drink" into Coffee, Dining, etc. — this is the value-add over Plaid's built-in categories.

### 4.6 API Key Management

Your Anthropic API key (for programmatic backend calls) is separate from a Claude.ai subscription. Get yours at [console.anthropic.com](https://console.anthropic.com). The key starts with `sk-ant-api03-...`.

> **Security warning:** If you ever accidentally commit an API key to Git, rotate it immediately. Go to the Anthropic console, delete the old key, and generate a new one. The same applies to Plaid credentials.

---

## Milestone 5: Authentication, API Endpoints, and Frontend

### 5.1 JWT Authentication — How It Works

JWT (JSON Web Token) is a stateless authentication mechanism:

```
1. User POSTs email + password to /api/auth/login
2. Backend validates credentials, generates a JWT containing the user ID
3. Frontend stores the JWT in localStorage
4. Every subsequent request includes: Authorization: Bearer <token>
5. Backend's JwtAuthenticationFilter intercepts each request,
   validates the token, and sets the SecurityContext
```

"Stateless" means the server doesn't store sessions. The JWT itself contains all the information needed to identify the user. This makes it easy to scale horizontally (multiple server instances).

### 5.2 JWT Utility Class

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Generate a signing key from the secret string
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(secret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### 5.3 JWT Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isValid(token)) {
                String userId = jwtUtil.extractUserId(token);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId, null, List.of()  // no roles needed
                    );
                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**How this works:** Every HTTP request passes through this filter. It extracts the JWT from the `Authorization` header, validates it, and puts the user ID into Spring's SecurityContext. Downstream controllers access it via `Authentication auth` parameter — `auth.getName()` returns the user ID.

### 5.4 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // Disable for REST APIs
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Public
                .anyRequest().authenticated()                  // Everything else needs JWT
            )
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 5.5 Auth Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        if (userRepo.findByEmail(body.get("email")).isPresent()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Email already registered"));
        }

        User user = User.builder()
            .email(body.get("email"))
            .name(body.get("name"))
            .passwordHash(passwordEncoder.encode(body.get("password")))
            .build();
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepo.findByEmail(body.get("email"));
        if (userOpt.isEmpty() ||
            !passwordEncoder.matches(body.get("password"),
                                     userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(userOpt.get().getId());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
```

### 5.6 Transaction and Budget Controllers

```java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired private TransactionRepository transactionRepo;

    @GetMapping
    public ResponseEntity<?> getTransactions(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(transactionRepo.findByUserId(userId));
    }

    @GetMapping("/by-category")
    public ResponseEntity<?> getByCategory(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<Object[]> results = transactionRepo.findSpendingByCategory(userId);
        // Transform Object[] into a cleaner response
        List<Map<String, Object>> data = results.stream()
            .map(row -> Map.of(
                "category", (String) row[0],
                "total", (BigDecimal) row[1]
            ))
            .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<?> getMonthlyTrends(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<Object[]> results = transactionRepo.findMonthlyTrends(userId);
        List<Map<String, Object>> data = results.stream()
            .map(row -> Map.of(
                "month", (String) row[0],
                "total", (BigDecimal) row[1]
            ))
            .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        List<Transaction> txns = transactionRepo.findByUserId(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Merchant,Amount,AI Category,Confidence\n");
        for (Transaction t : txns) {
            csv.append(String.format("%s,%s,%.2f,%s,%.2f\n",
                t.getDate(), t.getMerchantName(), t.getAmount(),
                t.getAiCategory(), t.getAiConfidence()));
        }

        return ResponseEntity.ok()
            .header("Content-Disposition",
                    "attachment; filename=transactions.csv")
            .header("Content-Type", "text/csv")
            .body(csv.toString().getBytes());
    }
}
```

```java
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired private BudgetRepository budgetRepo;

    @GetMapping
    public ResponseEntity<?> getBudgets(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(budgetRepo.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> createBudget(
            @RequestBody Budget budget, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        budget.setUser(User.builder().id(userId).build());
        return ResponseEntity.ok(budgetRepo.save(budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        budgetRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }
}
```

### 5.7 CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",   // Local React dev server
                "https://expense-tracker-iota-eight-49.vercel.app"  // Production
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
    }
}
```

**Why CORS?** Browsers block requests from one origin (localhost:3000) to another (localhost:8080) by default. This security feature prevents malicious sites from calling your API. You explicitly allow your frontend's origin.

### 5.8 Frontend: Axios API Service

```javascript
// services/api.js
import axios from 'axios';

const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api'
});

// Automatically attach JWT to every request
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// API functions
export const login = (email, password) =>
    api.post('/auth/login', { email, password });

export const register = (name, email, password) =>
    api.post('/auth/register', { name, email, password });

export const getTransactions = () => api.get('/transactions');
export const getByCategory = () => api.get('/transactions/by-category');
export const getMonthlyTrends = () => api.get('/transactions/monthly-trends');
export const exportCsv = () => api.get('/transactions/export',
    { responseType: 'blob' });

export const getBudgets = () => api.get('/budgets');
export const createBudget = (category, monthlyLimit) =>
    api.post('/budgets', { category, monthlyLimit });
export const deleteBudget = (id) => api.delete(`/budgets/${id}`);

export const createLinkToken = () => api.post('/plaid/create-link-token');
export const exchangeToken = (publicToken) =>
    api.post('/plaid/exchange-token', { public_token: publicToken });
export const syncTransactions = () => api.post('/plaid/sync-transactions');

export default api;
```

**The interceptor pattern** is key. Instead of manually adding `Authorization: Bearer <token>` to every API call, the interceptor does it automatically. This is a common pattern in production React apps.

### 5.9 Frontend: Key React Components

**Dashboard (main page):**

```jsx
function Dashboard() {
    const [transactions, setTransactions] = useState([]);
    const [categoryData, setCategoryData] = useState([]);
    const [trendData, setTrendData] = useState([]);
    const [budgets, setBudgets] = useState([]);

    useEffect(() => {
        // Load all data on mount
        Promise.all([
            getTransactions(),
            getByCategory(),
            getMonthlyTrends(),
            getBudgets()
        ]).then(([txnRes, catRes, trendRes, budgetRes]) => {
            setTransactions(txnRes.data);
            setCategoryData(catRes.data);
            setTrendData(trendRes.data);
            setBudgets(budgetRes.data);
        });
    }, []);

    return (
        <div>
            <PlaidLinkButton onSuccess={() => syncTransactions()
                .then(() => window.location.reload())} />
            <SpendingChart data={categoryData} />
            <MonthlyTrends data={trendData} />
            <BudgetAlerts budgets={budgets} categoryData={categoryData} />
            <TransactionList transactions={transactions} />
        </div>
    );
}
```

**Spending Pie Chart (Recharts):**

```jsx
import { PieChart, Pie, Cell, Tooltip, Legend } from 'recharts';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042',
                '#8884d8', '#82ca9d', '#ffc658', '#ff7300'];

function SpendingChart({ data }) {
    return (
        <PieChart width={400} height={400}>
            <Pie data={data} dataKey="total" nameKey="category"
                 cx="50%" cy="50%" outerRadius={120}>
                {data.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
            </Pie>
            <Tooltip formatter={(value) => `$${value.toFixed(2)}`} />
            <Legend />
        </PieChart>
    );
}
```

**Budget Alerts (with over-budget warnings):**

```jsx
function BudgetAlerts({ budgets, categoryData }) {
    return (
        <div>
            <h2>Budget Progress</h2>
            {budgets.map(budget => {
                const spent = categoryData.find(
                    c => c.category === budget.category
                )?.total || 0;
                const percentage = (spent / budget.monthlyLimit) * 100;
                const overBudget = percentage > 100;

                return (
                    <div key={budget.id}>
                        <span>{budget.category}: ${spent.toFixed(2)}
                              / ${budget.monthlyLimit}</span>
                        <div style={{
                            width: '100%', height: '20px',
                            background: '#e0e0e0', borderRadius: '10px'
                        }}>
                            <div style={{
                                width: `${Math.min(percentage, 100)}%`,
                                height: '100%',
                                background: overBudget ? '#ff4444' : '#4CAF50',
                                borderRadius: '10px'
                            }} />
                        </div>
                        {overBudget && (
                            <span style={{ color: 'red' }}>
                                Over budget by ${(spent - budget.monthlyLimit)
                                    .toFixed(2)}!
                            </span>
                        )}
                    </div>
                );
            })}
        </div>
    );
}
```

---

## Bonus: Docker & Deployment

### Docker Multi-Stage Build

```dockerfile
# Stage 1: Build the JAR
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw package -DskipTests

# Stage 2: Run with minimal image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why multi-stage?** The build stage includes the full JDK, Maven, and all source code (~500MB). The runtime stage only includes the JRE and the compiled JAR (~200MB). This cuts the Docker image size in half and reduces the attack surface.

### docker-compose.yml

```yaml
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/expense_tracker
      - SPRING_DATASOURCE_USERNAME=expense_user
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: expense_tracker
      POSTGRES_USER: expense_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U expense_user -d expense_tracker"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

**The health check** ensures the backend doesn't start before PostgreSQL is ready. Without it, the backend would crash on startup because the database isn't accepting connections yet.

### Deploy to Railway

1. Push your code to GitHub.
2. Create a new project on [railway.app](https://railway.app).
3. Add a **PostgreSQL** service (Railway provisions it automatically).
4. Add a **service from GitHub repo** pointing to your backend.
5. Set environment variables in Railway's dashboard using the reference variable syntax for the database URL:
   `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
6. Railway detects the Dockerfile and builds automatically.

### Deploy Frontend to Vercel

1. Push the `frontend/` directory to GitHub (or a separate repo).
2. Import the repo on [vercel.com](https://vercel.com).
3. Add the environment variable: `REACT_APP_API_URL=https://your-backend.up.railway.app/api`
4. Vercel auto-detects Create React App and deploys.

---

## Common Pitfalls & Debugging

| Problem                           | Cause                                             | Fix                                                          |
| --------------------------------- | ------------------------------------------------- | ------------------------------------------------------------ |
| `relation "users" does not exist` | Hibernate can't create tables                     | Check `ddl-auto: update` in application.yml and database permissions |
| Port 8080 already in use          | Previous process didn't stop                      | `lsof -i :8080` then `kill <PID>`                            |
| CORS errors in browser            | Backend doesn't allow frontend origin             | Add your frontend URL to `CorsConfig.java`                   |
| 403 on all API calls              | JWT token expired or missing                      | Check `localStorage` for token; clear and re-login           |
| Plaid Link won't open             | Invalid or expired link token                     | Ensure sandbox credentials are correct; check backend logs   |
| Claude API returns 401            | Wrong API key or using Claude.ai subscription key | Use an API key from `console.anthropic.com`, not a Claude.ai account |
| `Cannot deserialize` from Claude  | Response isn't clean JSON                         | Add `.replaceAll("```json", "")` cleanup before parsing      |
| Docker build fails on `mvnw`      | Permission denied                                 | Add `RUN chmod +x mvnw` before running it                    |
| Railway deployment stuck          | Missing environment variables                     | Check all 10 env vars are set in Railway dashboard           |

---

## Project File Structure (Complete)

```
expense-tracker/
├── backend/
│   ├── src/main/java/com/expensetracker/backend/
│   │   ├── BackendApplication.java
│   │   ├── config/
│   │   │   ├── PlaidConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── PlaidController.java
│   │   │   ├── TransactionController.java
│   │   │   └── BudgetController.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── PlaidAccount.java
│   │   │   ├── Transaction.java
│   │   │   ├── Budget.java
│   │   │   └── CategoryResult.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── PlaidAccountRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   └── BudgetRepository.java
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── service/
│   │       ├── PlaidService.java
│   │       └── AiCategorizationService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── .env                    ← gitignored
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Dashboard.jsx
│   │   │   ├── Login.jsx
│   │   │   ├── PlaidLink.jsx
│   │   │   ├── TransactionList.jsx
│   │   │   ├── SpendingChart.jsx
│   │   │   ├── MonthlyTrends.jsx
│   │   │   └── BudgetAlerts.jsx
│   │   ├── services/
│   │   │   └── api.js
│   │   ├── App.js
│   │   └── .env.production
│   └── package.json
├── .gitignore
└── README.md
```
