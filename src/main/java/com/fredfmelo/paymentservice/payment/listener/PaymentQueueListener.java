package com.fredfmelo.paymentservice.payment.listener;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.eventdrivencore.idempotency.executor.IdempotentExecutor;
import com.fredfmelo.paymentservice.payment.event.InventoryUnavailableEvent;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.service.PaymentService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentQueueListener {

    private static final String ORDER_CREATED = "ORDER_CREATED";
    private static final String INVENTORY_UNAVAILABLE = "INVENTORY_UNAVAILABLE";

    private final PaymentService paymentService;
    private final IdempotentExecutor idempotentExecutor;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.payment-queue}")
    public void consume(String payload) throws Exception {
        JsonNode eventNode = objectMapper.readTree(payload);
        String eventType = eventNode.get("eventType").asText();

        switch (eventType) {
            case ORDER_CREATED -> {
                OrderCreatedEvent event = objectMapper.treeToValue(eventNode, OrderCreatedEvent.class);
                idempotentExecutor.execute(event, () -> paymentService.processPayment(event));
            }
            case INVENTORY_UNAVAILABLE -> {
                InventoryUnavailableEvent event = objectMapper.treeToValue(eventNode, InventoryUnavailableEvent.class);
                idempotentExecutor.execute(event, () -> paymentService.processRefund(event));
            }
            default -> log.warn("Ignoring unsupported event type={}", eventType);
        }
    }
}
