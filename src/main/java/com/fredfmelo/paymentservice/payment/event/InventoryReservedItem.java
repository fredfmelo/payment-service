package com.fredfmelo.paymentservice.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryReservedItem(
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice,
        UUID sellerId) {
}
