package com.rabtech.order.domain.events;

import com.rabtech.order.domain.model.OrderId;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base interface for all domain events emitted by the Order aggregate.
 */
public interface DomainEvent extends Serializable {

    /**
     * Unique identifier of the Order associated with this event.
     */
    OrderId orderId();

    /**
     * Timestamp when the event occurred.
     */
    Instant occurredAt();
}
