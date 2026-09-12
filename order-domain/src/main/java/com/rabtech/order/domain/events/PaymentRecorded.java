package com.rabtech.order.domain.events;

import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event published when payment is successfully recorded against an order.
 * Requirement event: "PaymentRecorded"
 */
public record PaymentRecorded(
        OrderId orderId,
        String paymentReference,
        Money amountPaid,
        Instant occurredAt
) implements DomainEvent {

    public PaymentRecorded {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(paymentReference, "paymentReference cannot be null");
        Objects.requireNonNull(amountPaid, "amountPaid cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
