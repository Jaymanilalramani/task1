# ADR-002: Order Aggregate Root and Invariants Enforcement

## Status
Accepted

## Context
In Domain-Driven Design (DDD), an Aggregate is a cluster of domain objects that can be treated as a single unit for data changes. The Aggregate Root is the gateway through which all external callers must interact with the aggregate's internal state.

The brief requires the following invariants for the `Order` aggregate:
1. "An order must contain at least one line before confirmation."
2. "Quantity must be a positive whole number."
3. "A cancelled order cannot be paid."
4. "A paid order cannot return to draft."
5. "Order total is derived from immutable line prices and quantities."

## Decision
We make `Order` the sole Aggregate Root, encapsulating internal collections and properties:

1. **Encapsulation of Lines**: The internal `lines` list is `private final List<OrderLine>`. External callers can only access lines via an unmodifiable view (`Collections.unmodifiableList`).
2. **Controlled Mutations**:
   - Lines can only be added via `order.addLine(...)` and removed via `order.removeLine(...)`.
   - Both methods verify that the order is currently in `DRAFT` status; modifying lines in `CONFIRMED`, `PAID`, or `CANCELLED` status raises `InvalidOrderStateException`.
3. **Derived Order Total (Rule 5)**:
   - Total amount cannot be set directly from the outside.
   - Any addition or removal of a line automatically recalculates the order total by summing `line.lineTotal()` (which is `unitPrice * quantity`).
4. **Enforcing Confirmation Invariants (Rule 1)**:
   - `order.confirm(...)` explicitly checks `lines.isEmpty()`. If empty, it rejects the transition with `OrderValidationException("An order must contain at least one line before confirmation.")`.

## Consequences
### Positive
- Aggregate state is always valid at the end of every transaction.
- Invariants cannot be bypassed by external callers mutating collections directly.
- Rich domain model replaces anemic procedural services.

### Negative / Trade-offs
- Callers cannot perform bulk line mutations without calling aggregate methods.
