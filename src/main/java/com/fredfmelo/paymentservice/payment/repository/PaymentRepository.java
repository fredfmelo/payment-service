package com.fredfmelo.paymentservice.payment.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fredfmelo.paymentservice.config.ServiceConfig;
import com.fredfmelo.paymentservice.payment.domain.PaymentEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final ServiceConfig serviceConfig;
    private final DynamoDbEnhancedClient client;

    private DynamoDbTable<PaymentEntity> table() {
        return client.table(serviceConfig.getAws().getDynamodb().getTableName(),
                TableSchema.fromBean(PaymentEntity.class));
    }

    public void save(PaymentEntity entity) {
        table().putItem(entity);
    }

    public Optional<PaymentEntity> findByOrderId(String orderId) {
        DynamoDbIndex<PaymentEntity> index = table().index(PaymentEntity.ORDER_PAYMENTS_INDEX);

        QueryConditional query = QueryConditional.keyEqualTo(
                Key.builder()
                        .partitionValue(orderId)
                        .build());

        return index.query(r -> r.queryConditional(query))
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }
}
