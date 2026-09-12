package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.Quantity;

import java.util.Objects;

/**
 * Command to add an order line to an existing order in DRAFT status.
 */
public record AddOrderLineCommand(
        OrderId orderId,
        String productId,
        String description,
        Quantity quantity,
        Money unitPrice
) {
    public AddOrderLineCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(quantity, "quantity cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
    }
}
