package com.fredfmelo.paymentservice.infrastructure.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.paymentservice.config.ServiceConfig;
import com.fredfmelo.paymentservice.payment.event.PaymentApprovedEvent;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final SnsClient snsClient;
    private final ServiceConfig config;
    private final ObjectMapper objectMapper;

    public void publish(PaymentApprovedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(config.getSns().getOrderTopicArn())
                    .message(payload)
                    .messageAttributes(Map.of(
                            "eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(event.eventType())
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish event", ex);
        }
    }
}