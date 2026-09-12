# ADR-003: Modeling Value Objects with Immutability

## Status
Accepted

## Context
Primitive obsession (using raw `int`, `double`, or `String` for domain concepts) frequently leads to bugs such as:
- Negative or zero quantities causing erroneous totals.
- Floating-point inaccuracies when calculating monetary values with `double`.
- Missing currency checks leading to invalid financial aggregation (e.g. adding USD to EUR).
- Meaningless IDs (e.g. empty or null strings).

## Decision
We model domain concepts as self-validating, immutable Value Objects using Java 17 records and immutable classes:

1. **`Quantity`**:
   - Implemented as a Java `record Quantity(int value)`.
   - Compact constructor enforces Rule 2: `"Quantity must be a positive whole number."` (`value > 0`). Any input `<= 0` immediately throws `OrderValidationException`.
2. **`Money`**:
   - Implemented as a Java `record Money(BigDecimal amount, Currency currency)`.
   - Enforces scale of 2 with `RoundingMode.HALF_UP` to prevent monetary rounding bugs.
   - Enforces non-negative values.
   - Provides safe arithmetic methods (`plus`, `multiply`) that assert identical currency, preventing invalid mixed-currency operations.
3. **`OrderId` and `OrderLineId`**:
   - Strongly-typed identifier records enforcing non-null and non-blank values.
4. **`OrderLine`**:
   - Represents the item entity within the aggregate.
   - Computes immutable `lineTotal` as `unitPrice.multiply(quantity)` upon creation.

## Consequences
### Positive
- Invalid values are rejected immediately at creation time.
- Calculations are mathematically precise and currency-safe.
- Compile-time type safety prevents accidentally passing a `productId` where an `orderId` was expected.

### Negative / Trade-offs
- Slight object allocation overhead (negligible on modern JVMs with record scalarization).
