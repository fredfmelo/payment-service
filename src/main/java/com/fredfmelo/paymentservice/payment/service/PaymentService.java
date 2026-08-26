package com.fredfmelo.paymentservice.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.outbox.entity.OutboxEntity;
import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.paymentservice.payment.client.LedgerClient;
import com.fredfmelo.paymentservice.payment.client.LedgerMutationResponse;
import com.fredfmelo.paymentservice.payment.client.LedgerWalletResponse;
import com.fredfmelo.paymentservice.payment.domain.PaymentEntity;
import com.fredfmelo.paymentservice.payment.domain.PaymentStatus;
import com.fredfmelo.paymentservice.payment.event.InventoryReservedEvent;
import com.fredfmelo.paymentservice.payment.event.InventoryReservedItem;
import com.fredfmelo.paymentservice.payment.event.InventoryUnavailableEvent;
import com.fredfmelo.paymentservice.payment.event.OrderCreatedEvent;
import com.fredfmelo.paymentservice.payment.event.OrderItem;
import com.fredfmelo.paymentservice.payment.event.OrderItemEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentApprovedEvent;
import com.fredfmelo.paymentservice.payment.event.PaymentRefundedEvent;
import com.fredfmelo.paymentservice.payment.repository.PaymentRepository;
import com.fredfmelo.paymentservice.payment.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String ORDER_PAYMENT = "ORDER_PAYMENT";
    private static final String ORDER_REFUND = "ORDER_REFUND";
    private static final String SELLER_RECEIPT = "SELLER_RECEIPT";

    private final OutboxService outboxService;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final LedgerClient ledgerClient;

    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment order={} customer={}", event.orderId(), event.customerId());

        var existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.filter(p -> p.getStatus() == PaymentStatus.APPROVED).isPresent()) {
            log.info("Payment already approved for order={}, skipping", event.orderId());
            return;
        }

        BigDecimal total = calculateTotal(event);

        try {
            LedgerWalletResponse wallet = ledgerClient.getWalletByUserId(event.customerId());
            LedgerMutationResponse debit = ledgerClient.debit(
                    wallet.accountId(),
                    total,
                    ORDER_PAYMENT,
                    "Payment for order " + event.orderId());

            PaymentEntity payment = buildPayment(event, total, PaymentStatus.APPROVED, debit.transactionId());
            PaymentApprovedEvent paymentApprovedEvent = buildPaymentApprovedEvent(event);
            OutboxEntity outbox = outboxService.buildEntity(paymentApprovedEvent);
            paymentTransactionRepository.save(payment, outbox);

            log.info("Payment approved order={} amount={} ledgerTx={}",
                    event.orderId(), total, debit.transactionId());
        } catch (BusinessException ex) {
            if (ex.getHttpStatusCode() == HttpStatus.CONFLICT.value()
                    || ex.getHttpStatusCode() == HttpStatus.NOT_FOUND.value()) {
                saveFailedPayment(event, total);
                log.warn("Payment failed order={} reason={}", event.orderId(), ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    public void processRefund(InventoryUnavailableEvent event) {
        log.info("Processing refund order={} reason={}", event.orderId(), event.reason());

        PaymentEntity payment = paymentRepository.findByOrderId(event.orderId())
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND.value()));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already refunded for order={}, skipping", event.orderId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.warn("Cannot refund payment in status {} for order={}", payment.getStatus(), event.orderId());
            return;
        }

        LedgerWalletResponse wallet = ledgerClient.getWalletByUserId(payment.getCustomerId());
        ledgerClient.credit(
                wallet.accountId(),
                payment.getAmount(),
                ORDER_REFUND,
                "Refund for order " + event.orderId());

        payment.setStatus(PaymentStatus.REFUNDED);

        PaymentRefundedEvent paymentRefundedEvent = new PaymentRefundedEvent(
                UUID.randomUUID(),
                event.traceId(),
                "PAYMENT_REFUNDED",
                Instant.now(),
                event.orderId(),
                mapItems(event.items()),
                event.reason());

        OutboxEntity outbox = outboxService.buildEntity(paymentRefundedEvent);
        paymentTransactionRepository.save(payment, outbox);

        log.info("Payment refunded order={} amount={}", event.orderId(), payment.getAmount());
    }

    public void processSellerPayout(InventoryReservedEvent event) {
        log.info("Processing seller payout order={}", event.orderId());

        if (event.items() == null || event.items().isEmpty()) {
            log.warn("No items in INVENTORY_RESERVED for order={}, skipping seller payout", event.orderId());
            return;
        }

        Map<UUID, BigDecimal> payoutBySeller = event.items().stream()
                .filter(item -> item.sellerId() != null && item.unitPrice() != null)
                .collect(Collectors.groupingBy(
                        InventoryReservedItem::sellerId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())),
                                BigDecimal::add)));

        for (Map.Entry<UUID, BigDecimal> entry : payoutBySeller.entrySet()) {
            UUID sellerId = entry.getKey();
            BigDecimal amount = entry.getValue();

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LedgerWalletResponse wallet = ledgerClient.getWalletByUserId(sellerId);
            ledgerClient.credit(
                    wallet.accountId(),
                    amount,
                    SELLER_RECEIPT,
                    "Seller receipt for order " + event.orderId());

            log.info("Seller credited seller={} amount={} order={}", sellerId, amount, event.orderId());
        }
    }

    private BigDecimal calculateTotal(OrderCreatedEvent event) {
        if (event.totalAmount() != null) {
            return event.totalAmount();
        }

        return event.items().stream()
                .filter(item -> item.unitPrice() != null && item.quantity() != null)
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveFailedPayment(OrderCreatedEvent event, BigDecimal total) {
        PaymentEntity payment = buildPayment(event, total, PaymentStatus.FAILED, null);
        paymentRepository.save(payment);
    }

    private PaymentEntity buildPayment(
            OrderCreatedEvent event,
            BigDecimal total,
            PaymentStatus status,
            UUID ledgerTransactionId) {

        PaymentEntity payment = new PaymentEntity();
        payment.setPk("PAYMENT#" + UUID.randomUUID());
        payment.setSk("METADATA");
        payment.setOrderId(event.orderId());
        payment.setCustomerId(event.customerId());
        payment.setAmount(total);
        payment.setStatus(status);
        payment.setLedgerTransactionId(ledgerTransactionId);
        payment.setCreatedAt(Instant.now());
        return payment;
    }

    private PaymentApprovedEvent buildPaymentApprovedEvent(OrderCreatedEvent event) {
        return new PaymentApprovedEvent(
                UUID.randomUUID(),
                event.traceId(),
                "PAYMENT_APPROVED",
                Instant.now(),
                event.orderId(),
                event.items());
    }

    private List<OrderItemEvent> mapItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemEvent(UUID.fromString(item.productId()), item.quantity(), null, null))
                .toList();
    }
}
