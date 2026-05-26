package com.fredfmelo.paymentservice.idempotency.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fredfmelo.paymentservice.idempotency.entity.IdempotencyEntity;
import com.fredfmelo.paymentservice.idempotency.repository.IdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "IDEMPOTENCY#";
    private static final String METADATA = "METADATA";

    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";

    private static final String SERVICE = "payment-service";
    private static final long PROCESSING_TIMEOUT_MINUTES = 5;

    private final IdempotencyRepository repository;

    public boolean acquire(String eventId) {
        IdempotencyEntity existing = repository.find(eventId);

        if (existing != null) {

            if (PROCESSED.equals(existing.getStatus())) {
                return false;
            }

            if (PROCESSING.equals(existing.getStatus())) {
                long age = Duration.between(
                        existing.getUpdatedAt(),
                        Instant.now()
                ).toMinutes();

                return age > PROCESSING_TIMEOUT_MINUTES;
            }
        }

        IdempotencyEntity entity = new IdempotencyEntity();

        entity.setPk(PREFIX + eventId);
        entity.setSk(METADATA);
        entity.setService(SERVICE);

        entity.setStatus(PROCESSING);

        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        return repository.acquire(entity);
    }

    public void markProcessed(String eventId) {
        IdempotencyEntity entity = repository.find(eventId);

        entity.setStatus(PROCESSED);
        entity.setUpdatedAt(Instant.now());

        repository.save(entity);
    }
}