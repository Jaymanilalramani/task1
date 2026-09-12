package com.rabtech.order.domain;

import com.rabtech.order.domain.exceptions.OrderValidationException;
import com.rabtech.order.domain.model.Money;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.model.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Objects Invariant Tests")
class ValueObjectsTest {

    @Nested
    @DisplayName("OrderId Tests")
    class OrderIdTests {

        @Test
        @DisplayName("Valid OrderId generates and holds non-empty string")
        void validOrderId() {
            OrderId id1 = OrderId.generate();
            assertThat(id1.value()).isNotBlank();

            OrderId id2 = OrderId.of("ORD-12345");
            assertThat(id2.value()).isEqualTo("ORD-12345");
            assertThat(id2.toString()).isEqualTo("ORD-12345");
        }

        @Test
        @DisplayName("OrderId with null or blank string throws OrderValidationException")
        void invalidOrderIdThrowsException() {
            assertThatThrownBy(() -> OrderId.of(null))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("cannot be null or empty");

            assertThatThrownBy(() -> OrderId.of("   "))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Quantity Tests (Rule 2)")
    class QuantityTests {

        @Test
        @DisplayName("Valid positive whole numbers succeed")
        void validPositiveWholeNumber() {
            Quantity q1 = Quantity.of(1);
            Quantity q5 = Quantity.of(5);

            assertThat(q1.value()).isEqualTo(1);
            assertThat(q5.value()).isEqualTo(5);
            assertThat(q1.add(q5).value()).isEqualTo(6);
        }

        @ParameterizedTest(name = "Quantity {0} must throw exception")
        @ValueSource(ints = {0, -1, -5, -999})
        @DisplayName("Rule 2: Quantity must be a positive whole number")
        void zeroOrNegativeQuantityThrowsException(int invalidValue) {
            assertThatThrownBy(() -> Quantity.of(invalidValue))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessage("Quantity must be a positive whole number.");
        }
    }

    @Nested
    @DisplayName("Money Tests (Rule 5)")
    class MoneyTests {

        @Test
        @DisplayName("Valid Money with positive amount and currency scales to 2 decimal places")
        void validMoneyCreation() {
            Money m = Money.usd(19.999);
            assertThat(m.amount()).isEqualByComparingTo("20.00");
            assertThat(m.currency()).isEqualTo(Money.DEFAULT_CURRENCY);
        }

        @Test
        @DisplayName("Negative money amount throws OrderValidationException")
        void negativeMoneyThrowsException() {
            assertThatThrownBy(() -> Money.usd(-10.0))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("Arithmetic: plus and multiply enforce scale and currency")
        void arithmeticOperations() {
            Money m1 = Money.usd(15.50);
            Money m2 = Money.usd(10.25);

            Money sum = m1.plus(m2);
            assertThat(sum.amount()).isEqualByComparingTo("25.75");

            Money multiplied = m1.multiply(Quantity.of(3));
            assertThat(multiplied.amount()).isEqualByComparingTo("46.50");
        }

        @Test
        @DisplayName("Currency mismatch in arithmetic throws OrderValidationException")
        void currencyMismatchThrowsException() {
            Money usd = Money.of(BigDecimal.TEN, Currency.getInstance("USD"));
            Money eur = Money.of(BigDecimal.TEN, Currency.getInstance("EUR"));

            assertThatThrownBy(() -> usd.plus(eur))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }
}
