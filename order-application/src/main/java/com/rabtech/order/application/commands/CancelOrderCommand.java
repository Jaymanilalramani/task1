package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.OrderId;

import java.util.Objects;

/**
 * Command to cancel an order.
 */
public record CancelOrderCommand(OrderId orderId, String reason) {

    public CancelOrderCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
    }
}
