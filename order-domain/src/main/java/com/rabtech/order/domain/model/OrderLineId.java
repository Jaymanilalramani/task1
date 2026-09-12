package com.rabtech.order.domain.model;

import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value object representing the unique identifier of an OrderLine within an Order.
 */
public record OrderLineId(String value) implements Serializable {

    public OrderLineId {
        if (value == null || value.trim().isEmpty()) {
            throw new OrderValidationException("OrderLineId cannot be null or empty.");
        }
    }

    public static OrderLineId generate() {
        return new OrderLineId(UUID.randomUUID().toString());
    }

    public static OrderLineId of(String value) {
        return new OrderLineId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
