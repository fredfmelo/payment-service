package com.fredfmelo.paymentservice.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.exception.TechnicalException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LedgerClient {

    private final RestClient ledgerRestClient;

    public LedgerWalletResponse getWalletByUserId(UUID userId) {
        try {
            return ledgerRestClient.get()
                    .uri("/internal/users/{userId}/wallet", userId)
                    .retrieve()
                    .body(LedgerWalletResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapException(ex);
        }
    }

    public LedgerMutationResponse debit(UUID accountId, BigDecimal amount, String type, String description) {
        return mutate(accountId, amount, type, description, "debit");
    }

    public LedgerMutationResponse credit(UUID accountId, BigDecimal amount, String type, String description) {
        return mutate(accountId, amount, type, description, "credit");
    }

    private LedgerMutationResponse mutate(
            UUID accountId,
            BigDecimal amount,
            String type,
            String description,
            String operation) {

        LedgerMutationRequest request = new LedgerMutationRequest(amount, type, description);

        try {
            return ledgerRestClient.post()
                    .uri("/internal/accounts/{accountId}/" + operation, accountId)
                    .body(request)
                    .retrieve()
                    .body(LedgerMutationResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapException(ex);
        }
    }

    private RuntimeException mapException(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();

        if (status == HttpStatus.CONFLICT.value()) {
            return new BusinessException("Insufficient balance", status);
        }

        if (status == HttpStatus.NOT_FOUND.value()) {
            return new BusinessException(extractMessage(body, "Resource not found"), status);
        }

        return new TechnicalException("Ledger service error: " + body, status);
    }

    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        return body;
    }
}
