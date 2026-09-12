package com.rabtech.order.domain.exceptions;

/**
 * Thrown when an illegal state transition is attempted on an Order.
 * Examples:
 * - A cancelled order cannot be paid.
 * - A paid order cannot return to draft.
 */
public class InvalidOrderStateException extends OrderDomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
