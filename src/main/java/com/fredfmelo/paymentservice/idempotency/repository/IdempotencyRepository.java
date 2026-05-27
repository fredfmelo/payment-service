package com.fredfmelo.paymentservice.idempotency.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.fredfmelo.paymentservice.config.ServiceConfig;
import com.fredfmelo.paymentservice.idempotency.entity.IdempotencyEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final ServiceConfig serviceConfig;
    private final DynamoDbEnhancedClient client;

    private DynamoDbTable<IdempotencyEntity> table() {
        return client.table(
                serviceConfig.getDynamodb().getTableName(),
                TableSchema.fromBean(IdempotencyEntity.class));
    }

    public boolean acquire(IdempotencyEntity entity) {
        try {
            PutItemEnhancedRequest<IdempotencyEntity> request = PutItemEnhancedRequest.builder(
                    IdempotencyEntity.class)
                    .item(entity)
                    .conditionExpression(
                            Expression.builder().expression("attribute_not_exists(pk)").build())
                    .build();

            table().putItem(request);

            return true;

        } catch (ConditionalCheckFailedException ex) {
            return false;
        }
    }

    public void save(IdempotencyEntity entity) {
        table().putItem(entity);
    }

    public IdempotencyEntity find(UUID eventId) {

        return table().getItem(
                Key.builder().partitionValue("IDEMPOTENCY#" + eventId)
                        .sortValue("METADATA")
                        .build());
    }
}