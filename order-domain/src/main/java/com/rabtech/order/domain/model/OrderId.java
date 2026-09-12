package com.rabtech.order.domain.model;

import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an Order.
 */
public record OrderId(String value) implements Serializable {

    public OrderId {
        if (value == null || value.trim().isEmpty()) {
            throw new OrderValidationException("OrderId cannot be null or empty.");
        }
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
