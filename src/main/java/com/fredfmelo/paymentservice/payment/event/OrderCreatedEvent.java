package com.fredfmelo.paymentservice.payment.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fredfmelo.eventdrivencore.event.Event;

public record OrderCreatedEvent(UUID eventId,
        String eventType,
        String traceId,
        Instant occurredAt,
        String orderId,
        UUID customerId,
        List<OrderItemEvent> items) implements Event {
}