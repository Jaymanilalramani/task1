package com.rabtech.order.domain.exceptions;

/**
 * Thrown when domain validation constraints are violated.
 * Examples:
 * - An order must contain at least one line before confirmation.
 * - Quantity must be a positive whole number.
 */
public class OrderValidationException extends OrderDomainException {

    public OrderValidationException(String message) {
        super(message);
    }
}
