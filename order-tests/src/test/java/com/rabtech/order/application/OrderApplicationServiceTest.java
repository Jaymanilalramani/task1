package com.rabtech.order.application;

import com.rabtech.order.adapters.inmemory.InMemoryOrderRepository;
import com.rabtech.order.adapters.inmemory.RecordingNotificationAdapter;
import com.rabtech.order.adapters.time.TestTimeProvider;
import com.rabtech.order.application.commands.AddOrderLineCommand;
import com.rabtech.order.application.commands.CancelOrderCommand;
import com.rabtech.order.application.commands.ConfirmOrderCommand;
import com.rabtech.order.application.commands.CreateOrderCommand;
import com.rabtech.order.application.commands.RecordPaymentCommand;
import com.rabtech.order.application.service.OrderApplicationService;
import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.OrderStatus;
import com.rabtech.order.domain.model.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderApplicationService Integration with Ports and Adapters")
class OrderApplicationServiceTest {

    private InMemoryOrderRepository orderRepository;
    private RecordingNotificationAdapter notificationAdapter;
    private TestTimeProvider timeProvider;
    private OrderApplicationService service;

    private final Instant baseTime = Instant.parse("2026-05-01T09:00:00Z");

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        notificationAdapter = new RecordingNotificationAdapter();
        timeProvider = new TestTimeProvider(baseTime);
        service = new OrderApplicationService(orderRepository, notificationAdapter, timeProvider);
    }

    @Test
    @DisplayName("End-to-End Successful Workflow: Create -> Add Line -> Confirm -> Pay")
    void fullApprovalLifecycle() {
        OrderId orderId = OrderId.of("ORD-WF-001");

        // 1. Create order
        service.handle(CreateOrderCommand.of(orderId));
        assertThat(orderRepository.existsById(orderId)).isTrue();
        Order createdOrder = service.getOrder(orderId);
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(createdOrder.createdAt()).isEqualTo(baseTime);

        // Advance clock by 10 minutes
        timeProvider.advance(Duration.ofMinutes(10));

        // 2. Add line
        service.handle(new AddOrderLineCommand(
                orderId,
                "PROD-LAPTOP",
                "Developer Laptop 16-inch",
                Quantity.of(1),
                Money.usd(1500.00)
        ));

        Order orderWithLine = service.getOrder(orderId);
        assertThat(orderWithLine.lines()).hasSize(1);
        assertThat(orderWithLine.totalAmount()).isEqualTo(Money.usd(1500.00));

        // Advance clock by 5 minutes
        timeProvider.advance(Duration.ofMinutes(5));
        Instant confirmTime = timeProvider.now();

        // 3. Confirm order
        service.handle(new ConfirmOrderCommand(orderId));
        Order confirmedOrder = service.getOrder(orderId);
        assertThat(confirmedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(notificationAdapter.getConfirmedEvents()).hasSize(1);
        assertThat(notificationAdapter.getConfirmedEvents().get(0).orderId()).isEqualTo(orderId);
        assertThat(notificationAdapter.getConfirmedEvents().get(0).occurredAt()).isEqualTo(confirmTime);

        // Advance clock by 1 hour
        timeProvider.advance(Duration.ofHours(1));
        Instant payTime = timeProvider.now();

        // 4. Record payment
        service.handle(new RecordPaymentCommand(orderId, "WIRE-778899", Money.usd(1500.00)));
        Order paidOrder = service.getOrder(orderId);
        assertThat(paidOrder.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOrder.paymentReference()).isEqualTo("WIRE-778899");
        assertThat(notificationAdapter.getPaymentEvents()).hasSize(1);
        assertThat(notificationAdapter.getPaymentEvents().get(0).occurredAt()).isEqualTo(payTime);

        // Total notifications sent = 2 (OrderConfirmed + PaymentRecorded)
        assertThat(notificationAdapter.totalNotificationsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cancellation Workflow: Create -> Add Line -> Cancel")
    void cancellationLifecycle() {
        OrderId orderId = OrderId.of("ORD-WF-002");

        service.handle(CreateOrderCommand.of(orderId));
        service.handle(new AddOrderLineCommand(
                orderId,
                "PROD-MOUSE",
                "Wireless Mouse",
                Quantity.of(2),
                Money.usd(25.00)
        ));

        timeProvider.advance(Duration.ofMinutes(30));
        Instant cancelTime = timeProvider.now();

        service.handle(new CancelOrderCommand(orderId, "Discontinued item"));

        Order cancelledOrder = service.getOrder(orderId);
        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.cancellationReason()).isEqualTo("Discontinued item");

        assertThat(notificationAdapter.getCancelledEvents()).hasSize(1);
        assertThat(notificationAdapter.getCancelledEvents().get(0).orderId()).isEqualTo(orderId);
        assertThat(notificationAdapter.getCancelledEvents().get(0).reason()).isEqualTo("Discontinued item");
        assertThat(notificationAdapter.getCancelledEvents().get(0).occurredAt()).isEqualTo(cancelTime);
    }
}
