package com.rabtech.order.domain.model;

import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable entity representing a line item in an Order.
 * Invariants:
 * - LineId, ProductId, and Description cannot be null or empty.
 * - Quantity must be positive (enforced by Quantity value object).
 * - Unit price cannot be null or negative (enforced by Money value object).
 * - Line total is strictly derived as unitPrice * quantity.
 */
public final class OrderLine implements Serializable {

    private final OrderLineId id;
    private final String productId;
    private final String description;
    private final Quantity quantity;
    private final Money unitPrice;
    private final Money lineTotal;

    public OrderLine(OrderLineId id, String productId, String description, Quantity quantity, Money unitPrice) {
        if (id == null) {
            throw new OrderValidationException("OrderLine id cannot be null.");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new OrderValidationException("ProductId cannot be null or empty.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new OrderValidationException("Description cannot be null or empty.");
        }
        if (quantity == null) {
            throw new OrderValidationException("Quantity cannot be null.");
        }
        if (unitPrice == null) {
            throw new OrderValidationException("UnitPrice cannot be null.");
        }

        this.id = id;
        this.productId = productId.trim();
        this.description = description.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity);
    }

    public OrderLineId id() {
        return id;
    }

    public String productId() {
        return productId;
    }

    public String description() {
        return description;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Money lineTotal() {
        return lineTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine orderLine = (OrderLine) o;
        return Objects.equals(id, orderLine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderLine{" +
                "id=" + id +
                ", productId='" + productId + '\'' +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                '}';
    }
}
