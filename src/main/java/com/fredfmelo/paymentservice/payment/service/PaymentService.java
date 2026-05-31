package com.fredfmelo.paymentservice.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
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

    private void createOutboxEvent(OrderCreatedEvent orderCreatedEvent) {
        PaymentApprovedEvent paymentApprovedEvent = new PaymentApprovedEvent(
                UUID.randomUUID(),
                orderCreatedEvent.traceId(),
                "PAYMENT_APPROVED",
                Instant.now(),
                orderCreatedEvent.orderId());

        //TODO: when the real payment structure is define, replace this save with a transactionalRepository that saves the business and outbox entity in the same transaction
        outboxService.save(paymentApprovedEvent);
    }

    private void simulatePayment() {
        // TODO: implement ledger / provider / anti-fraud
        log.info("[BUSINESS-FLOW-PLACEHOLDER] Simulating payment...");
    }
}