package com.fredfmelo.paymentservice.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.paymentservice.outbox.service.OutboxService;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentApprovedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OutboxService outboxService;

    public void process(OrderCreatedEvent event) {
        log.info("Processing payment order={} customer={}",
                event.orderId(),
                event.customerId());

        simulatePayment();

        createOutboxEvent(event);
    }

    private void createOutboxEvent(OrderCreatedEvent event) {
        PaymentApprovedEvent approved = new PaymentApprovedEvent(
                UUID.randomUUID(),
                "PAYMENT_APPROVED",
                Instant.now(),
                event.orderId());

        log.info("Payment approved {}", approved);

        outboxService.save(approved.eventId().toString(),
                approved.eventType(),
                approved);
    }

    private void simulatePayment() {
        // later: ledger / provider / anti-fraud
    }
}