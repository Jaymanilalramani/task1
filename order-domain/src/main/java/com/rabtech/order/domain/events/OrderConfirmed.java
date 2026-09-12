package com.rabtech.order.domain.events;

import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event published when an order is confirmed.
 * Requirement event: "OrderConfirmed"
 */
public record OrderConfirmed(
        OrderId orderId,
        Money totalAmount,
        int lineCount,
        Instant occurredAt
) implements DomainEvent {

    public OrderConfirmed {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
