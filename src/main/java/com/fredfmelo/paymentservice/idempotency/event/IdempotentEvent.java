package com.fredfmelo.paymentservice.idempotency.event;

import java.util.UUID;

public interface IdempotentEvent {
    UUID eventId();
}
