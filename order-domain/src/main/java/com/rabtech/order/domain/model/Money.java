package com.rabtech.order.domain.model;

import com.rabtech.order.domain.exceptions.OrderValidationException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing a monetary amount with currency.
 * Invariants:
 * - Amount cannot be null or negative.
 * - Arithmetic operations only allowed between identical currencies.
 * - Maintained at scale of 2 with HALF_UP rounding.
 */
public record Money(BigDecimal amount, Currency currency) implements Serializable, Comparable<Money> {

    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");
    public static final Money ZERO_USD = new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);

    public Money {
        if (amount == null) {
            throw new OrderValidationException("Money amount cannot be null.");
        }
        if (currency == null) {
            throw new OrderValidationException("Money currency cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderValidationException("Money amount cannot be negative: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money usd(String amount) {
        return new Money(new BigDecimal(amount), DEFAULT_CURRENCY);
    }

    public Money plus(Money other) {
        if (other == null) {
            throw new OrderValidationException("Cannot add null Money.");
        }
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(Quantity quantity) {
        if (quantity == null) {
            throw new OrderValidationException("Cannot multiply Money by null Quantity.");
        }
        return multiply(quantity.value());
    }

    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new OrderValidationException("Cannot multiply Money by negative number: " + multiplier);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        if (other == null) {
            throw new OrderValidationException("Cannot compare against null Money.");
        }
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isEqualTo(Money other) {
        if (other == null) {
            return false;
        }
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) == 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new OrderValidationException(
                    "Currency mismatch: Cannot operate between " + this.currency + " and " + other.currency);
        }
    }

    @Override
    public int compareTo(Money o) {
        Objects.requireNonNull(o, "Cannot compare Money with null");
        assertSameCurrency(o);
        return this.amount.compareTo(o.amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
