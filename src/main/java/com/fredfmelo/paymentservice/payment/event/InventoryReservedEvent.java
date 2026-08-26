package com.fredfmelo.paymentservice.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fredfmelo.eventdrivencore.event.Event;

public record InventoryReservedEvent(UUID eventId,
        String traceId,
        String eventType,
        Instant occurredAt,
        String orderId,
        BigDecimal totalAmount,
        List<InventoryReservedItem> items) implements Event {
}
