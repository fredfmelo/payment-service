package com.fredfmelo.paymentservice.payment.client;

import java.math.BigDecimal;

public record LedgerMutationRequest(
        BigDecimal amount,
        String type,
        String description) {
}
