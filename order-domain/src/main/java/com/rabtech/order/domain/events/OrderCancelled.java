package com.rabtech.order.domain.events;

import com.rabtech.order.domain.model.OrderId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event published when an order is cancelled.
 * Requirement event: "OrderCancelled"
 */
public record OrderCancelled(
        OrderId orderId,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public OrderCancelled {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
