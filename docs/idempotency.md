# Idempotent Consumer Pattern

This project uses the Idempotent Consumer pattern to safely process duplicate messages in an event-driven architecture.

## Problem

Systems using SNS/SQS operate under an **at-least-once delivery** model.

This means duplicate events are expected.

Example:

```text
OrderCreatedEvent
        ↓
payment-service processes event
        ↓
service crashes before ACK
        ↓
SQS redelivers same message
```

Without protection:

```text
eventId=123
    ↓
charge payment

eventId=123
    ↓
charge payment again ❌
```

The same operation could execute multiple times.

---

## Solution

Each consumed event creates an idempotency record using the eventId:

```text
pk=IDEMPOTENCY#123
sk=METADATA
```

This record acts as a logical processing lock.

---

## Flow

```text
Receive message
      ↓
Try atomic acquire
      ↓
Record exists?
   ↙          ↘
No             Yes
↓               ↓
Create          Inspect state
PROCESSING      ↓
↓               Duplicate?
Process           ↓
↓              Ignore
Success?
   ↙
No
↓
SQS retries

Yes
↓
Mark PROCESSED
```

---

## States

### PROCESSING

Indicates a consumer has acquired ownership of processing.

Example:

```text
pk=IDEMPOTENCY#123
status=PROCESSING
updatedAt=2026-05-27T10:00:00Z
```

---

### PROCESSED

Indicates processing completed successfully.

```text
pk=IDEMPOTENCY#123
status=PROCESSED
updatedAt=2026-05-27T10:00:10Z
```

Future duplicate messages are ignored.

---

## Atomic Acquisition

The implementation uses DynamoDB conditional writes:

```java
conditionExpression(
    "attribute_not_exists(pk)"
)
```

Behavior:

```text
Thread A
    ↓
create IDEMPOTENCY#123
SUCCESS

Thread B
    ↓
create IDEMPOTENCY#123
FAIL
```

Only one consumer wins.

No waiting occurs.

No database lock exists.

This is optimistic concurrency.

---

## Why not use a database lock?

Pessimistic locking:

```text
Thread A acquires lock
Thread B waits
```

Problems:

- lower throughput
- lock management complexity
- unsuitable for distributed consumers

This project uses:

```text
Idempotent Consumer
+
Optimistic Concurrency
+
Conditional Writes
```

---

## Retry Recovery

A consumer may crash after acquiring ownership:

```text
PROCESSING
    ↓
service crashes
```

To avoid permanent stuck states:

```text
PROCESSING older than timeout
        ↓
allow retry
```

Current timeout:

```text
5 minutes
```

---

## Benefits

- Prevents duplicate processing
- Supports retries safely
- Handles concurrent consumers
- Works with SNS/SQS at-least-once delivery
- Avoids distributed locks
- Production-ready event handling

---

## Related Patterns

This project uses:

```text
Transactional Outbox
        +
Idempotent Consumer
```

Outbox guarantees events are not lost.

Idempotency guarantees retries remain safe.

Together they form a reliable event-driven architecture.