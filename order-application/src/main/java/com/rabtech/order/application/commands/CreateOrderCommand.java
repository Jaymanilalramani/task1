package com.rabtech.order.application.commands;

import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;

import java.util.Currency;
import java.util.Objects;

/**
 * Command to create a new order in DRAFT status.
 */
public record CreateOrderCommand(OrderId orderId, Currency currency) {

    public CreateOrderCommand {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        if (currency == null) {
            currency = Money.DEFAULT_CURRENCY;
        }
    }

    public static CreateOrderCommand of(OrderId orderId) {
        return new CreateOrderCommand(orderId, Money.DEFAULT_CURRENCY);
    }
}
