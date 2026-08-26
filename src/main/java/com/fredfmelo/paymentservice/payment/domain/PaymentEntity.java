package com.fredfmelo.paymentservice.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@DynamoDbBean
public class PaymentEntity {

    public static final String ORDER_PAYMENTS_INDEX = "order-payments-index";

    private String pk;
    private String sk;
    private String orderId;
    private UUID customerId;
    private BigDecimal amount;
    private PaymentStatus status;
    private UUID ledgerTransactionId;
    private Instant createdAt;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = ORDER_PAYMENTS_INDEX)
    public String getOrderId() {
        return orderId;
    }
}
