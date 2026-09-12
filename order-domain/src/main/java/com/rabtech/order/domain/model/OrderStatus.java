package com.rabtech.order.domain.model;

/**
 * Enumeration of allowable states in the Order approval workflow.
 * Specified states: DRAFT, CONFIRMED, PAID, CANCELLED.
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    PAID,
    CANCELLED;

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isTerminal() {
        return this == PAID || this == CANCELLED;
    }
}
