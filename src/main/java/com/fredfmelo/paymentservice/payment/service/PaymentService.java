package com.fredfmelo.paymentservice.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.paymentservice.infrastructure.messaging.PaymentEventPublisher;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentApprovedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentEventPublisher paymentEventPublisher;

    public void process(OrderCreatedEvent event) {
        log.info("Processing payment order={} customer={}",
                event.orderId(),
                event.customerId());

        simulatePayment();

        PaymentApprovedEvent approved = new PaymentApprovedEvent(UUID.randomUUID(),
            "PAYMENT_APPROVED",
            Instant.now(),
            event.orderId());

        log.info("Payment approved {}", approved);

        paymentEventPublisher.publish(approved);
    }

    private void simulatePayment() {
        // later: ledger / provider / anti-fraud
    }

}