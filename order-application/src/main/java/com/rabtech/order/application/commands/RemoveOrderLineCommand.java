package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.OrderLineId;

import java.util.Objects;

/**
 * Command to remove an order line from an order in DRAFT status.
 */
public record RemoveOrderLineCommand(OrderId orderId, OrderLineId lineId) {

    public RemoveOrderLineCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(lineId, "lineId cannot be null");
    }
}
