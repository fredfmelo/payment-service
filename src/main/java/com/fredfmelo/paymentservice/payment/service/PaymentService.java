package com.fredfmelo.paymentservice.payment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.paymentservice.payment.event.InventoryUnavailableEvent;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.event.OrderItem;
import com.fredfmelo.paymentservice.payment.event.OrderItemEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentApprovedEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentRefundedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OutboxService outboxService;

    public void processPayment(OrderCreatedEvent event) {
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
                orderCreatedEvent.orderId(),
                orderCreatedEvent.items());

        //TODO: when the real payment structure is define, replace this save with a transactionalRepository that saves the business and outbox entity in the same transaction
        outboxService.save(paymentApprovedEvent);
    }

    public void processRefund(InventoryUnavailableEvent event) {
        log.info("Processing refund order={} reason={}",
                event.orderId(),
                event.reason());

        simulateRefund();

        createRefundOutboxEvent(event);
    }

    private void createRefundOutboxEvent(InventoryUnavailableEvent event) {
        PaymentRefundedEvent paymentRefundedEvent = new PaymentRefundedEvent(
                UUID.randomUUID(),
                event.traceId(),
                "PAYMENT_REFUNDED",
                Instant.now(),
                event.orderId(),
                mapItems(event.items()),
                event.reason());

        //TODO: when the real payment structure is define, replace this save with a transactionalRepository that saves the business and outbox entity in the same transaction
        outboxService.save(paymentRefundedEvent);
    }

    private List<OrderItemEvent> mapItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemEvent(UUID.fromString(item.productId()), item.quantity()))
                .toList();
    }

    private void simulatePayment() {
        // TODO: implement ledger / provider / anti-fraud
        log.info("[BUSINESS-FLOW-PLACEHOLDER] Simulating payment...");
    }

    private void simulateRefund() {
        // TODO: implement ledger / provider / anti-fraud
        log.info("[BUSINESS-FLOW-PLACEHOLDER] Simulating refund...");
    }
}