package com.fredfmelo.paymentservice.payment.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LedgerMutationResponse(
        UUID transactionId,
        UUID accountId,
        String type,
        String entryType,
        BigDecimal amount,
        BigDecimal balance,
        String currency,
        OffsetDateTime createdAt) {
}
