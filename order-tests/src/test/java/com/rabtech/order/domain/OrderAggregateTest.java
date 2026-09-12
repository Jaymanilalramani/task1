package com.rabtech.order.domain;

import com.rabtech.order.domain.events.DomainEvent;
import com.rabtech.order.domain.events.OrderCancelled;
import com.rabtech.order.domain.events.OrderConfirmed;
import com.rabtech.order.domain.events.PaymentRecorded;
import com.rabtech.order.domain.exceptions.InvalidOrderStateException;
import com.rabtech.order.domain.exceptions.OrderValidationException;
import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.OrderLineId;
import com.rabtech.order.domain.model.OrderStatus;
import com.rabtech.order.domain.model.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order Aggregate Root & Domain Invariants Tests")
class OrderAggregateTest {

    private final Instant now = Instant.parse("2026-03-01T10:00:00Z");
    private OrderId orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = OrderId.of("ORD-TEST-001");
        order = Order.create(orderId, now);
    }

    @Test
    @DisplayName("Newly created order starts in DRAFT status with zero total and no lines")
    void orderInitialization() {
        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.lines()).isEmpty();
        assertThat(order.totalAmount().amount()).isEqualByComparingTo("0.00");
        assertThat(order.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Rule 1: An order must contain at least one line before confirmation")
    void rule1_orderMustContainAtLeastOneLineBeforeConfirmation() {
        // Attempt confirmation with 0 lines
        assertThatThrownBy(() -> order.confirm(now))
                .isInstanceOf(OrderValidationException.class)
                .hasMessage("An order must contain at least one line before confirmation.");

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Rule 5: Order total is derived from immutable line prices and quantities")
    void rule5_orderTotalIsDerivedFromImmutableLinePricesAndQuantities() {
        // Line 1: 2 units * $15.50 = $31.00
        order.addLine("PROD-1", "Mechanical Keyboard", Quantity.of(2), Money.usd(15.50), now);
        assertThat(order.totalAmount().amount()).isEqualByComparingTo("31.00");

        // Line 2: 3 units * $9.99 = $29.97
        order.addLine("PROD-2", "Optical Mouse", Quantity.of(3), Money.usd(9.99), now);
        assertThat(order.totalAmount().amount()).isEqualByComparingTo("60.97");

        // Line 3: 1 unit * $40.00 = $40.00
        OrderLineId line3Id = order.addLine("PROD-3", "Desk Mat", Quantity.of(1), Money.usd(40.00), now);
        assertThat(order.totalAmount().amount()).isEqualByComparingTo("100.97");

        // Verify line total of individual line
        assertThat(order.lines().get(0).lineTotal().amount()).isEqualByComparingTo("31.00");
        assertThat(order.lines().get(1).lineTotal().amount()).isEqualByComparingTo("29.97");
        assertThat(order.lines().get(2).lineTotal().amount()).isEqualByComparingTo("40.00");

        // Remove Line 3 -> total decreases back to $60.97
        order.removeLine(line3Id, now);
        assertThat(order.lines()).hasSize(2);
        assertThat(order.totalAmount().amount()).isEqualByComparingTo("60.97");
    }

    @Test
    @DisplayName("Confirming order transitions status to CONFIRMED and emits OrderConfirmed event")
    void confirmOrderEmitsEvent() {
        order.addLine("PROD-A", "Item A", Quantity.of(1), Money.usd(50.00), now);
        order.confirm(now);

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderConfirmed.class);

        OrderConfirmed event = (OrderConfirmed) events.get(0);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.totalAmount()).isEqualTo(Money.usd(50.00));
        assertThat(event.lineCount()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Recording payment transitions status to PAID and emits PaymentRecorded event")
    void recordPaymentEmitsEvent() {
        order.addLine("PROD-A", "Item A", Quantity.of(2), Money.usd(50.00), now);
        order.confirm(now);
        order.clearDomainEvents();

        order.recordPayment("TXN-98765", Money.usd(100.00), now);

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.paymentReference()).isEqualTo("TXN-98765");

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentRecorded.class);

        PaymentRecorded event = (PaymentRecorded) events.get(0);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.paymentReference()).isEqualTo("TXN-98765");
        assertThat(event.amountPaid()).isEqualTo(Money.usd(100.00));
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Recording payment with insufficient amount throws OrderValidationException")
    void recordPaymentWithInsufficientAmountThrowsException() {
        order.addLine("PROD-A", "Item A", Quantity.of(2), Money.usd(50.00), now);
        order.confirm(now);

        assertThatThrownBy(() -> order.recordPayment("TXN-1", Money.usd(80.00), now))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("is insufficient to pay order total");
    }

    @Test
    @DisplayName("Cancelling order transitions status to CANCELLED and emits OrderCancelled event")
    void cancelOrderEmitsEvent() {
        order.addLine("PROD-A", "Item A", Quantity.of(1), Money.usd(25.00), now);
        order.cancel("Customer request", now);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellationReason()).isEqualTo("Customer request");

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderCancelled.class);

        OrderCancelled event = (OrderCancelled) events.get(0);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.reason()).isEqualTo("Customer request");
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Attempting to cancel already cancelled order throws InvalidOrderStateException")
    void cancelAlreadyCancelledOrderThrowsException() {
        order.cancel("First reason", now);

        assertThatThrownBy(() -> order.cancel("Second reason", now))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order is already cancelled.");
    }

    @Test
    @DisplayName("Attempting to cancel PAID order throws InvalidOrderStateException")
    void cancelPaidOrderThrowsException() {
        order.addLine("PROD-A", "Item A", Quantity.of(1), Money.usd(25.00), now);
        order.confirm(now);
        order.recordPayment("TXN-PAID", Money.usd(25.00), now);

        assertThatThrownBy(() -> order.cancel("Refund request", now))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Cannot cancel an order that has already been PAID.");
    }
}
