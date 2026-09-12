package com.rabtech.order.application.service;

import com.rabtech.order.application.commands.AddOrderLineCommand;
import com.rabtech.order.application.commands.CancelOrderCommand;
import com.rabtech.order.application.commands.ConfirmOrderCommand;
import com.rabtech.order.application.commands.CreateOrderCommand;
import com.rabtech.order.application.commands.RecordPaymentCommand;
import com.rabtech.order.application.commands.RemoveOrderLineCommand;
import com.rabtech.order.domain.events.DomainEvent;
import com.rabtech.order.domain.events.OrderCancelled;
import com.rabtech.order.domain.events.OrderConfirmed;
import com.rabtech.order.domain.events.PaymentRecorded;
import com.rabtech.order.domain.exceptions.OrderValidationException;
import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.OrderLineId;
import com.rabtech.order.domain.ports.NotificationPort;
import com.rabtech.order.domain.ports.OrderRepository;
import com.rabtech.order.domain.ports.TimeProvider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Application Service orchestrating the Order approval workflow.
 * Uses pure Java constructor-based dependency injection to consume ports.
 */
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final NotificationPort notificationPort;
    private final TimeProvider timeProvider;

    public OrderApplicationService(
            OrderRepository orderRepository,
            NotificationPort notificationPort,
            TimeProvider timeProvider
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository cannot be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort cannot be null");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider cannot be null");
    }

    public OrderId handle(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        if (orderRepository.existsById(command.orderId())) {
            throw new OrderValidationException("Order already exists with ID: " + command.orderId());
        }

        Instant now = timeProvider.now();
        Order order = Order.create(command.orderId(), command.currency(), now);
        orderRepository.save(order);
        dispatchEvents(order);
        return order.id();
    }

    public OrderLineId handle(AddOrderLineCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Order order = loadOrder(command.orderId());
        OrderLineId lineId = order.addLine(
                command.productId(),
                command.description(),
                command.quantity(),
                command.unitPrice(),
                timeProvider.now()
        );
        orderRepository.save(order);
        dispatchEvents(order);
        return lineId;
    }

    public void handle(RemoveOrderLineCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Order order = loadOrder(command.orderId());
        order.removeLine(command.lineId(), timeProvider.now());
        orderRepository.save(order);
        dispatchEvents(order);
    }

    public void handle(ConfirmOrderCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Order order = loadOrder(command.orderId());
        order.confirm(timeProvider.now());
        orderRepository.save(order);
        dispatchEvents(order);
    }

    public void handle(RecordPaymentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Order order = loadOrder(command.orderId());
        order.recordPayment(command.paymentReference(), command.amountPaid(), timeProvider.now());
        orderRepository.save(order);
        dispatchEvents(order);
    }

    public void handle(CancelOrderCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Order order = loadOrder(command.orderId());
        order.cancel(command.reason(), timeProvider.now());
        orderRepository.save(order);
        dispatchEvents(order);
    }

    public Order getOrder(OrderId orderId) {
        return loadOrder(orderId);
    }

    private Order loadOrder(OrderId orderId) {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderValidationException("Order not found with ID: " + orderId));
    }

    private void dispatchEvents(Order order) {
        List<DomainEvent> events = order.getDomainEvents();
        for (DomainEvent event : events) {
            if (event instanceof OrderConfirmed oc) {
                notificationPort.notifyOrderConfirmed(oc);
            } else if (event instanceof PaymentRecorded pr) {
                notificationPort.notifyPaymentRecorded(pr);
            } else if (event instanceof OrderCancelled cancel) {
                notificationPort.notifyOrderCancelled(cancel);
            }
        }
        order.clearDomainEvents();
    }
}
