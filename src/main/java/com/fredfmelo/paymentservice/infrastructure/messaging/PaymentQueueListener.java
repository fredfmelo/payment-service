package com.fredfmelo.paymentservice.infrastructure.messaging;

import org.springframework.stereotype.Component;

import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentQueueListener {

    @SqsListener("${aws.sqs.payment-queue}")
    public void consume(OrderCreatedEvent event) {

        log.info(
                "Received order={} customer={} items={}",
                event.orderId(),
                event.customerId(),
                event.items().size()
        );
    }
}