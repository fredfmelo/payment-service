package com.fredfmelo.paymentservice.infrastructure.messaging;

import org.springframework.stereotype.Component;

import com.fredfmelo.paymentservice.idempotency.executor.IdempotentExecutor;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.service.PaymentService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentQueueListener {

    private final PaymentService paymentService;
    private final IdempotentExecutor idempotentExecutor;

    @SqsListener("${aws.sqs.payment-queue}")
    public void consume(OrderCreatedEvent event) {
        idempotentExecutor.execute(event, () -> paymentService.process(event));
    }
}