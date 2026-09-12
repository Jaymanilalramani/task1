# ADR-004: Explicit State Transitions and Domain Exceptions

## Status
Accepted

## Context
An Order progresses through distinct lifecycle phases: `DRAFT`, `CONFIRMED`, `PAID`, and `CANCELLED`.
Allowing arbitrary or implicit state mutations creates corrupted business records (e.g. paying an order that was cancelled, or modifying items after payment).

The domain requirements state:
- `"A cancelled order cannot be paid."`
- `"A paid order cannot return to draft."`

## Decision
We implement state transitions using domain verbs on the `Order` aggregate rather than exposing status setters:

```
                  +-----------------------------------+
                  |                                   |
                  v                                   |
              [ DRAFT ] ------------------------> [ CANCELLED ]
                  |       (cancel)                    ^
                  | (confirm)                         |
                  v                                   | (cancel)
            [ CONFIRMED ] ----------------------------+
                  |
                  | (recordPayment)
                  v
               [ PAID ] (terminal)
```

1. **Explicit Transition Methods**:
   - `confirm()`: Valid only from `DRAFT`. Emits `OrderConfirmed`.
   - `recordPayment()`: Valid only from `CONFIRMED`. Emits `PaymentRecorded`.
   - `cancel()`: Valid from `DRAFT` or `CONFIRMED`. Emits `OrderCancelled`.
   - `revertToDraft()`: Valid from `CONFIRMED`. Disallowed once `PAID` or `CANCELLED`.

2. **Rejection of Illegal Transitions**:
   - Calling `recordPayment()` on a `CANCELLED` order throws `InvalidOrderStateException("A cancelled order cannot be paid.")`.
   - Calling `revertToDraft()` or modifying lines on a `PAID` order throws `InvalidOrderStateException("A paid order cannot return to draft.")`.
   - Calling `recordPayment()` on a `DRAFT` order throws `InvalidOrderStateException("Cannot record payment for order in status: DRAFT. Order must be CONFIRMED.")`.
   - Calling `cancel()` or `recordPayment()` on an already `PAID` order throws `InvalidOrderStateException`.
   - Calling `confirm()` on a `CANCELLED` order throws `InvalidOrderStateException`.

## Consequences
### Positive
- Strict, provable state machine transitions validated by unit tests.
- Clear error messages describing business violations instead of cryptic generic failures.
- Prevents double payment and unauthorized modifications to fulfilled orders.

### Negative / Trade-offs
- Additional checks required in each state transition method.
