package com.expensetracker.backend.service;

import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.*;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service  // Tells Spring: "This is a service — create one instance and inject it where needed"
public class PlaidService {

    private final PlaidApi plaidApi;

    // Constructor injection: Spring automatically provides the PlaidApi bean
    public PlaidService(PlaidApi plaidApi) {
        this.plaidApi = plaidApi;
    }

    /**
     * STEP 1: Create a link_token.
     * The frontend needs this to open the Plaid Link widget.
     *
     * Think of it as a "session key" — it tells Plaid:
     * "This user from THIS app wants to connect a bank."
     */
    public String createLinkToken(String userId) throws IOException {
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .user(new LinkTokenCreateRequestUser().clientUserId(userId))
            .clientName("My Expense Tracker")
            .products(Arrays.asList(Products.TRANSACTIONS))  // We want transaction data
            .countryCodes(Arrays.asList(CountryCode.US))
            .language("en");

        Response<LinkTokenCreateResponse> response =
            plaidApi.linkTokenCreate(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Plaid linkTokenCreate failed: " +
                response.errorBody().string());
        }

        return response.body().getLinkToken();
    }

    /**
     * STEP 5: Exchange the temporary public_token for a permanent access_token.
     *
     * public_token is like a movie ticket — one-time use.
     * access_token is like a membership card — use it forever.
     */
    public ItemPublicTokenExchangeResponse exchangePublicToken(String publicToken)
            throws IOException {
        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
            .publicToken(publicToken);

        Response<ItemPublicTokenExchangeResponse> response =
            plaidApi.itemPublicTokenExchange(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Token exchange failed: " +
                response.errorBody().string());
        }

        return response.body();
        // Returns: { access_token: "access-sandbox-...", item_id: "..." }
    }

    /**
     * STEP 6: Fetch transactions using the access_token.
     *
     * We specify a date range. Plaid returns every transaction
     * the user made in that period across all connected accounts.
     */
    public List<com.plaid.client.model.Transaction> getTransactions(
            String accessToken, LocalDate start, LocalDate end) throws IOException {

        TransactionsGetRequest request = new TransactionsGetRequest()
            .accessToken(accessToken)
            .startDate(start)
            .endDate(end);

        Response<TransactionsGetResponse> response =
            plaidApi.transactionsGet(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to get transactions: " +
                response.errorBody().string());
        }

        return response.body().getTransactions();
    }
}