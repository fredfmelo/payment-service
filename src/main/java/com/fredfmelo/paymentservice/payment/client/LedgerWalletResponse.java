package com.fredfmelo.paymentservice.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerWalletResponse(
        UUID accountId,
        UUID userId,
        BigDecimal balance,
        String currency) {
}
