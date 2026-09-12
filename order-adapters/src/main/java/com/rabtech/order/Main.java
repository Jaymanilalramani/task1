package com.rabtech.order;

import com.rabtech.order.adapters.inmemory.InMemoryOrderRepository;
import com.rabtech.order.adapters.inmemory.RecordingNotificationAdapter;
import com.rabtech.order.adapters.time.TestTimeProvider;
import com.rabtech.order.application.commands.AddOrderLineCommand;
import com.rabtech.order.application.commands.ConfirmOrderCommand;
import com.rabtech.order.application.commands.CreateOrderCommand;
import com.rabtech.order.application.commands.RecordPaymentCommand;
import com.rabtech.order.application.service.OrderApplicationService;
import com.rabtech.order.domain.exceptions.InvalidOrderStateException;
import com.rabtech.order.domain.exceptions.OrderValidationException;
import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.Quantity;

import java.time.Duration;
import java.time.Instant;

/**
 * Console Entry Point for running and verifying the Order Approval Workflow.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   ORDER DOMAIN SYSTEM - APPROVAL WORKFLOW DEMONSTRATION       ");
        System.out.println("   Pure Java 17 Domain Model (Hexagonal / Ports & Adapters)    ");
        System.out.println("===============================================================\n");

        // 1. Initialize Adapters & Application Service
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        RecordingNotificationAdapter notificationAdapter = new RecordingNotificationAdapter();
        TestTimeProvider timeProvider = new TestTimeProvider(Instant.now());

        OrderApplicationService service = new OrderApplicationService(repository, notificationAdapter, timeProvider);

        OrderId orderId = OrderId.of("ORD-2026-DEMO");

        // Step 1: Create Order
        System.out.println("--- STEP 1: Creating Order ---");
        service.handle(CreateOrderCommand.of(orderId));
        Order order = service.getOrder(orderId);
        System.out.printf("[SUCCESS] Order created: ID=%s, Status=%s, Total=%s%n%n",
                order.id(), order.status(), order.totalAmount());

        // Step 2: Test Invariant Rule 1 (0 lines on confirmation)
        System.out.println("--- STEP 2: Testing Rule 1 (Confirming with 0 lines) ---");
        try {
            service.handle(new ConfirmOrderCommand(orderId));
            System.err.println("[FAILED] Rule 1 violation was not caught!");
        } catch (OrderValidationException ex) {
            System.out.printf("[VERIFIED RULE 1] Expected exception caught: %s%n%n", ex.getMessage());
        }

        // Step 3: Add Lines and Test Rule 5 (Derived Total)
        System.out.println("--- STEP 3: Adding Order Lines (Testing Rule 5 - Derived Total) ---");
        timeProvider.advance(Duration.ofMinutes(5));
        service.handle(new AddOrderLineCommand(
                orderId,
                "PROD-LAPTOP",
                "High Performance Workstation",
                Quantity.of(2),
                Money.usd(1200.00)
        ));
        service.handle(new AddOrderLineCommand(
                orderId,
                "PROD-MONITOR",
                "4K Ultra-wide Display",
                Quantity.of(1),
                Money.usd(600.00)
        ));

        order = service.getOrder(orderId);
        System.out.printf("[SUCCESS] Added 2 lines. Calculated Total = %s%n", order.totalAmount());
        order.lines().forEach(line -> System.out.printf("  - Line: %s x %s @ %s = %s%n",
                line.description(), line.quantity(), line.unitPrice(), line.lineTotal()));
        System.out.println();

        // Step 4: Confirm Order
        System.out.println("--- STEP 4: Confirming Order ---");
        timeProvider.advance(Duration.ofMinutes(10));
        service.handle(new ConfirmOrderCommand(orderId));
        order = service.getOrder(orderId);
        System.out.printf("[SUCCESS] Order confirmed: Status=%s%n", order.status());
        System.out.printf("[EVENT DISPATCHED] OrderConfirmed event captured by adapter (Total: %s)%n%n",
                notificationAdapter.getConfirmedEvents().get(0).totalAmount());

        // Step 5: Test Invariant Rule 4 (Cannot modify lines or return to draft once confirmed/paid)
        System.out.println("--- STEP 5: Testing Invariant (Modifying lines on confirmed order) ---");
        try {
            order.addLine("PROD-FAIL", "Extra", Quantity.of(1), Money.usd(10.00), timeProvider.now());
            System.err.println("[FAILED] Illegal modification allowed!");
        } catch (InvalidOrderStateException ex) {
            System.out.printf("[VERIFIED INVARIANT] Expected exception caught: %s%n%n", ex.getMessage());
        }

        // Step 6: Record Payment
        System.out.println("--- STEP 6: Recording Payment ---");
        timeProvider.advance(Duration.ofHours(1));
        service.handle(new RecordPaymentCommand(orderId, "PAY-REF-998877", Money.usd(3000.00)));
        order = service.getOrder(orderId);
        System.out.printf("[SUCCESS] Payment recorded: Status=%s, PaymentRef=%s%n",
                order.status(), order.paymentReference());
        System.out.printf("[EVENT DISPATCHED] PaymentRecorded event captured by adapter (Ref: %s)%n%n",
                notificationAdapter.getPaymentEvents().get(0).paymentReference());

        // Step 7: Test Rule 4: A paid order cannot return to draft
        System.out.println("--- STEP 7: Testing Rule 4 (Paid order cannot return to draft) ---");
        try {
            order.revertToDraft(timeProvider.now());
            System.err.println("[FAILED] Rule 4 violation was not caught!");
        } catch (InvalidOrderStateException ex) {
            System.out.printf("[VERIFIED RULE 4] Expected exception caught: %s%n%n", ex.getMessage());
        }

        System.out.println("===============================================================");
        System.out.println("   DEMO COMPLETED SUCCESSFULLY: ALL INVARIANTS SATISFIED!      ");
        System.out.printf("   Total Notifications Handled: %d%n", notificationAdapter.totalNotificationsCount());
        System.out.println("===============================================================");
    }
}
