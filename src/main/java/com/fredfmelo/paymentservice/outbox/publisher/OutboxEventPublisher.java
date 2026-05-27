package com.fredfmelo.paymentservice.outbox.publisher;

public interface OutboxEventPublisher {

    void publish(String payload, String eventType);
}