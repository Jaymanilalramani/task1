package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;

import java.util.Objects;

/**
 * Command to record payment for an order.
 */
public record RecordPaymentCommand(
        OrderId orderId,
        String paymentReference,
        Money amountPaid
) {
    public RecordPaymentCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(paymentReference, "paymentReference cannot be null");
        Objects.requireNonNull(amountPaid, "amountPaid cannot be null");
    }
}
