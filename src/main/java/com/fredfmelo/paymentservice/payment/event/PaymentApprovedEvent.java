package com.fredfmelo.paymentservice.payment.event;

import java.time.Instant;
import java.util.UUID;

import com.fredfmelo.paymentservice.idempotency.event.IdempotentEvent;

public record PaymentApprovedEvent(UUID eventId,
                String eventType,
                Instant occurredAt,
                String orderId) implements IdempotentEvent {
}