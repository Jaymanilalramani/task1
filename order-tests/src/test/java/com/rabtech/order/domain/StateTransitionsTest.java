package com.rabtech.order.domain;

import com.rabtech.order.domain.exceptions.InvalidOrderStateException;
import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.OrderStatus;
import com.rabtech.order.domain.model.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order State Transitions Test Suite (Legal and Illegal Proofs)")
class StateTransitionsTest {

    private final Instant now = Instant.parse("2026-06-15T12:00:00Z");
    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.create(OrderId.of("ORD-STATE-TEST"), now);
        order.addLine("SKU-1", "Sample Item", Quantity.of(2), Money.usd(50.00), now);
    }

    // =========================================================================
    // Legal Transitions
    // =========================================================================

    @Nested
    @DisplayName("Legal State Transitions")
    class LegalTransitions {

        @Test
        @DisplayName("Legal: DRAFT -> CONFIRMED")
        void draftToConfirmed() {
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
            order.confirm(now);
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Legal: CONFIRMED -> PAID")
        void confirmedToPaid() {
            order.confirm(now);
            order.recordPayment("TXN-100", Money.usd(100.00), now);
            assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("Legal: DRAFT -> CANCELLED")
        void draftToCancelled() {
            order.cancel("Customer changed mind", now);
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Legal: CONFIRMED -> CANCELLED")
        void confirmedToCancelled() {
            order.confirm(now);
            order.cancel("Out of stock inventory", now);
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Legal: CONFIRMED -> DRAFT (revert before payment)")
        void confirmedToDraft() {
            order.confirm(now);
            order.revertToDraft(now);
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        }
    }

    // =========================================================================
    // Illegal Transitions
    // =========================================================================

    @Nested
    @DisplayName("Illegal State Transitions")
    class IllegalTransitions {

        @Test
        @DisplayName("Rule 3: A cancelled order cannot be paid (from DRAFT -> CANCELLED)")
        void rule3_cancelledFromDraftCannotBePaid() {
            order.cancel("Changed mind", now);
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);

            assertThatThrownBy(() -> order.recordPayment("TXN-ILLEGAL", Money.usd(100.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("A cancelled order cannot be paid.");
        }

        @Test
        @DisplayName("Rule 3: A cancelled order cannot be paid (from CONFIRMED -> CANCELLED)")
        void rule3_cancelledFromConfirmedCannotBePaid() {
            order.confirm(now);
            order.cancel("Inventory shortage", now);
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);

            assertThatThrownBy(() -> order.recordPayment("TXN-ILLEGAL", Money.usd(100.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("A cancelled order cannot be paid.");
        }

        @Test
        @DisplayName("Rule 4: A paid order cannot return to draft (via revertToDraft)")
        void rule4_paidOrderCannotReturnToDraft() {
            order.confirm(now);
            order.recordPayment("TXN-SUCCESS", Money.usd(100.00), now);
            assertThat(order.status()).isEqualTo(OrderStatus.PAID);

            assertThatThrownBy(() -> order.revertToDraft(now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("A paid order cannot return to draft.");
        }

        @Test
        @DisplayName("Rule 4: A paid order cannot return to draft (via modifying lines)")
        void rule4_paidOrderCannotModifyLines() {
            order.confirm(now);
            order.recordPayment("TXN-SUCCESS", Money.usd(100.00), now);
            assertThat(order.status()).isEqualTo(OrderStatus.PAID);

            assertThatThrownBy(() -> order.addLine("SKU-EXTRA", "Extra", Quantity.of(1), Money.usd(10.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("A paid order cannot return to draft.");
        }

        @Test
        @DisplayName("Illegal: DRAFT cannot be directly PAID without confirmation")
        void draftCannotBeDirectlyPaid() {
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);

            assertThatThrownBy(() -> order.recordPayment("TXN-EARLY", Money.usd(100.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Order must be CONFIRMED");
        }

        @Test
        @DisplayName("Illegal: PAID order cannot be paid again")
        void paidOrderCannotBePaidAgain() {
            order.confirm(now);
            order.recordPayment("TXN-1", Money.usd(100.00), now);

            assertThatThrownBy(() -> order.recordPayment("TXN-2", Money.usd(100.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("Order is already paid.");
        }

        @Test
        @DisplayName("Illegal: CANCELLED order cannot be confirmed")
        void cancelledOrderCannotBeConfirmed() {
            order.cancel("Customer cancellation", now);

            assertThatThrownBy(() -> order.confirm(now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Cannot confirm order in status: CANCELLED");
        }

        @Test
        @DisplayName("Illegal: CONFIRMED order cannot have lines added")
        void confirmedOrderCannotAddLines() {
            order.confirm(now);

            assertThatThrownBy(() -> order.addLine("SKU-X", "New Item", Quantity.of(1), Money.usd(20.00), now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Cannot modify lines on an order with status: CONFIRMED");
        }

        @Test
        @DisplayName("Illegal: CANCELLED order cannot return to draft")
        void cancelledOrderCannotReturnToDraft() {
            order.cancel("Customer cancellation", now);

            assertThatThrownBy(() -> order.revertToDraft(now))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessage("A cancelled order cannot return to draft.");
        }
    }
}
