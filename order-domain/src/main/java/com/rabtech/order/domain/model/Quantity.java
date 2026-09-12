package com.rabtech.order.domain.model;

import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;

/**
 * Value Object representing a quantity.
 * Business Rule: "Quantity must be a positive whole number."
 */
public record Quantity(int value) implements Serializable, Comparable<Quantity> {

    public Quantity {
        if (value <= 0) {
            throw new OrderValidationException("Quantity must be a positive whole number.");
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        if (other == null) {
            throw new OrderValidationException("Cannot add null Quantity.");
        }
        return new Quantity(this.value + other.value);
    }

    @Override
    public int compareTo(Quantity o) {
        return Integer.compare(this.value, o.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
