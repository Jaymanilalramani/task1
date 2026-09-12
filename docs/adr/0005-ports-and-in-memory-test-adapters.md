# ADR-005: Ports and In-Memory Test Adapters

## Status
Accepted

## Context
A major challenge in testing enterprise systems is reliance on external infrastructure:
- Slow relational database setup or Docker testcontainers.
- Mocking frameworks (e.g. Mockito) resulting in brittle tests that test method invocation rather than behavior.
- Non-deterministic tests caused by calling `Instant.now()` or `System.currentTimeMillis()`.
- Untested notification and event dispatching.

## Decision
We define clean Outbound Ports in the domain and lightweight In-Memory Adapters for testing:

1. **`OrderRepository` Port**:
   - Outbound persistence contract (`save`, `findById`, `existsById`).
   - Implemented by `InMemoryOrderRepository` using a thread-safe `ConcurrentHashMap`.
   - Offers instant state verification and isolated test runs without DB overhead.

2. **`NotificationPort` Port**:
   - Outbound port for notifying external systems of domain events (`notifyOrderConfirmed`, `notifyPaymentRecorded`, `notifyOrderCancelled`).
   - Implemented by `RecordingNotificationAdapter` which records events in thread-safe lists, exposing inspection helpers for test assertions.

3. **`TimeProvider` Port**:
   - Abstract clock contract (`Instant now()`).
   - Implemented in production by `SystemTimeProvider` (`Instant.now()`).
   - Implemented in tests by `TestTimeProvider`, allowing tests to freeze, fast-forward, or manipulate time deterministically.

## Consequences
### Positive
- **Blazing Fast Test Suite**: The entire multi-module test suite executes in less than 300 milliseconds.
- **Deterministic Time**: Order creation, confirmation, and payment timestamps can be precisely correlated and asserted.
- **No Mockito Needed**: Pure fake adapters provide real, stateful behavior without complex mocking setups.
- **Pluggable Architecture**: Swapping `InMemoryOrderRepository` with PostgreSQL, DynamoDB, or Kafka requires creating a new adapter without touching a single line of domain code.

### Negative / Trade-offs
- In-memory adapters must be maintained alongside the domain interface contracts.
