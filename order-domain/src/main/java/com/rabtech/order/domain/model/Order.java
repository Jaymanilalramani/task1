package com.rabtech.order.domain.model;

import com.rabtech.order.domain.events.DomainEvent;
import com.rabtech.order.domain.events.OrderCancelled;
import com.rabtech.order.domain.events.OrderConfirmed;
import com.rabtech.order.domain.events.PaymentRecorded;
import com.rabtech.order.domain.exceptions.InvalidOrderStateException;
import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root: Order
 *
 * Implements the lifecycle: DRAFT -> CONFIRMED -> PAID or CANCELLED.
 * Enforces all domain rules and invariants:
 * 1. "An order must contain at least one line before confirmation."
 * 2. "Quantity must be a positive whole number."
 * 3. "A cancelled order cannot be paid."
 * 4. "A paid order cannot return to draft."
 * 5. "Order total is derived from immutable line prices and quantities."
 */
public class Order implements Serializable {

    private final OrderId id;
    private OrderStatus status;
    private final List<OrderLine> lines = new ArrayList<>();
    private Money totalAmount;
    private final Currency currency;
    private String paymentReference;
    private String cancellationReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    private Order(OrderId id, Currency currency, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "OrderId cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.status = OrderStatus.DRAFT;
        this.totalAmount = new Money(java.math.BigDecimal.ZERO, currency);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = createdAt;
    }

    /**
     * Factory method to initiate a new Order in DRAFT status.
     */
    public static Order create(OrderId id, Instant createdAt) {
        return new Order(id, Money.DEFAULT_CURRENCY, createdAt);
    }

    public static Order create(OrderId id, Currency currency, Instant createdAt) {
        return new Order(id, currency, createdAt);
    }

    // =========================================================================
    // Domain Business Operations
    // =========================================================================

    /**
     * Adds an order line to the order while in DRAFT status.
     * Recomputes order total from immutable line prices and quantities.
     */
    public OrderLineId addLine(String productId, String description, Quantity quantity, Money unitPrice, Instant now) {
        ensureCanModifyLines();
        Objects.requireNonNull(unitPrice, "UnitPrice cannot be null");
        if (!unitPrice.currency().equals(this.currency)) {
            throw new OrderValidationException(
                    "Line currency " + unitPrice.currency() + " does not match order currency " + this.currency);
        }

        OrderLineId lineId = OrderLineId.generate();
        OrderLine line = new OrderLine(lineId, productId, description, quantity, unitPrice);
        this.lines.add(line);
        recalculateTotal();
        this.updatedAt = Objects.requireNonNull(now, "Timestamp cannot be null");
        return lineId;
    }

    /**
     * Removes an order line by ID while in DRAFT status.
     * Recomputes order total.
     */
    public void removeLine(OrderLineId lineId, Instant now) {
        ensureCanModifyLines();
        Objects.requireNonNull(lineId, "OrderLineId cannot be null");

        boolean removed = this.lines.removeIf(line -> line.id().equals(lineId));
        if (!removed) {
            throw new OrderValidationException("Order line not found: " + lineId);
        }
        recalculateTotal();
        this.updatedAt = Objects.requireNonNull(now, "Timestamp cannot be null");
    }

    /**
     * Confirms the order.
     * Business Rule 1: "An order must contain at least one line before confirmation."
     */
    public void confirm(Instant now) {
        Objects.requireNonNull(now, "Timestamp cannot be null");

        if (this.status != OrderStatus.DRAFT) {
            throw new InvalidOrderStateException("Cannot confirm order in status: " + this.status);
        }

        if (this.lines.isEmpty()) {
            throw new OrderValidationException("An order must contain at least one line before confirmation.");
        }

        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = now;

        registerEvent(new OrderConfirmed(this.id, this.totalAmount, this.lines.size(), now));
    }

    /**
     * Records payment for the order.
     * Business Rule 3: "A cancelled order cannot be paid."
     */
    public void recordPayment(String paymentReference, Money amountPaid, Instant now) {
        Objects.requireNonNull(now, "Timestamp cannot be null");

        // Rule 3: A cancelled order cannot be paid.
        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("A cancelled order cannot be paid.");
        }

        if (this.status == OrderStatus.PAID) {
            throw new InvalidOrderStateException("Order is already paid.");
        }

        if (this.status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Cannot record payment for order in status: " + this.status + ". Order must be CONFIRMED.");
        }

        if (paymentReference == null || paymentReference.trim().isEmpty()) {
            throw new OrderValidationException("Payment reference cannot be null or empty.");
        }

        if (amountPaid == null) {
            throw new OrderValidationException("Payment amount cannot be null.");
        }

        if (!amountPaid.isGreaterThanOrEqualTo(this.totalAmount)) {
            throw new OrderValidationException(
                    "Payment amount " + amountPaid + " is insufficient to pay order total " + this.totalAmount);
        }

        this.status = OrderStatus.PAID;
        this.paymentReference = paymentReference.trim();
        this.updatedAt = now;

        registerEvent(new PaymentRecorded(this.id, this.paymentReference, amountPaid, now));
    }

    /**
     * Reverts order to draft.
     * Business Rule 4: "A paid order cannot return to draft."
     */
    public void revertToDraft(Instant now) {
        Objects.requireNonNull(now, "Timestamp cannot be null");

        // Rule 4: A paid order cannot return to draft.
        if (this.status == OrderStatus.PAID) {
            throw new InvalidOrderStateException("A paid order cannot return to draft.");
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("A cancelled order cannot return to draft.");
        }

        if (this.status == OrderStatus.DRAFT) {
            return; // Already in draft
        }

        this.status = OrderStatus.DRAFT;
        this.updatedAt = now;
    }

    /**
     * Cancels the order.
     */
    public void cancel(String reason, Instant now) {
        Objects.requireNonNull(now, "Timestamp cannot be null");

        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled.");
        }

        if (this.status == OrderStatus.PAID) {
            throw new InvalidOrderStateException("Cannot cancel an order that has already been PAID.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new OrderValidationException("Cancellation reason cannot be null or empty.");
        }

        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason.trim();
        this.updatedAt = now;

        registerEvent(new OrderCancelled(this.id, this.cancellationReason, now));
    }

    // =========================================================================
    // Invariant Helpers
    // =========================================================================

    private void ensureCanModifyLines() {
        // Business Rule 4 enforcement: Once paid, cannot be modified/returned to draft.
        if (this.status == OrderStatus.PAID) {
            throw new InvalidOrderStateException("A paid order cannot return to draft.");
        }
        if (this.status != OrderStatus.DRAFT) {
            throw new InvalidOrderStateException("Cannot modify lines on an order with status: " + this.status);
        }
    }

    /**
     * Business Rule 5: "Order total is derived from immutable line prices and quantities."
     */
    private void recalculateTotal() {
        Money runningTotal = new Money(java.math.BigDecimal.ZERO, this.currency);
        for (OrderLine line : lines) {
            runningTotal = runningTotal.plus(line.lineTotal());
        }
        this.totalAmount = runningTotal;
    }

    // =========================================================================
    // Domain Events Handling
    // =========================================================================

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(new ArrayList<>(domainEvents));
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // =========================================================================
    // Getters
    // =========================================================================

    public OrderId id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Currency currency() {
        return currency;
    }

    public String paymentReference() {
        return paymentReference;
    }

    public String cancellationReason() {
        return cancellationReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
