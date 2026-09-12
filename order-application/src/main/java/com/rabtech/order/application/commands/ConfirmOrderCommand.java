package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.OrderId;

import java.util.Objects;

/**
 * Command to confirm an order.
 */
public record ConfirmOrderCommand(OrderId orderId) {

    public ConfirmOrderCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
    }
}
