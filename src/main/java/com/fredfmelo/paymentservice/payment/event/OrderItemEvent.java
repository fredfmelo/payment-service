package com.fredfmelo.paymentservice.payment.event;

import java.util.UUID;

public record OrderItemEvent(
        UUID productId,
        Integer quantity) {}