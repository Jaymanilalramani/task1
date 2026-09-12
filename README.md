Order Domain System - Hexagonal Architecture & Approval Lifecycle
A framework-independent domain model implementing the Order approval workflow in Java 17, designed according to Domain-Driven Design (DDD) and Hexagonal (Ports and Adapters) Architecture principles.

1. Domain Requirements & Invariants
Based on order-domain-requirements.json:

Rule	Description	Implementation & Invariant Enforcement
Rule 1	An order must contain at least one line before confirmation.	Order.confirm() validates lines.isEmpty(), throwing OrderValidationException.
Rule 2	Quantity must be a positive whole number.	Quantity value object compact constructor requires value > 0, throwing OrderValidationException.
Rule 3	A cancelled order cannot be paid.	Order.recordPayment() checks status == CANCELLED, throwing InvalidOrderStateException.
Rule 4	A paid order cannot return to draft.	Order.revertToDraft() and Order.addLine() check status == PAID, throwing InvalidOrderStateException.
Rule 5	Order total is derived from immutable line prices and quantities.	Recalculated dynamically as ∑(line.unitPrice×line.quantity) upon adding/removing lines.
Constraint	Keep domain module independent of Spring, JPA, HTTP, DB.	Pure Java 17 standard library in order-domain (validated by DomainBoundaryTest).
2. State Machine
Plaintext
                    +------------------------------------+
                    |                                    |
                    v                                    |
                [ DRAFT ] ------------------------> [ CANCELLED ]
                    |        (cancel)                    ^
                    | (confirm)                          |
                    v                                    | (cancel)
              [ CONFIRMED ] -----------------------------+
                    |
                    | (recordPayment)
                    v
                 [ PAID ] (terminal state)
Transition Rules:

DRAFT: Can add/remove lines. Can transition to CONFIRMED (if lines ≥1) or CANCELLED. Direct payment is rejected.

CONFIRMED: Lines are immutable. Can transition to PAID (with valid payment) or CANCELLED, or revert to DRAFT.

PAID: Terminal state. Cannot return to draft, cannot add lines, cannot be paid again, cannot be cancelled.

CANCELLED: Terminal state. Cannot be paid, cannot be confirmed, cannot return to draft.

3. Multi-Module Project Structure
Plaintext
order-domain-system/
├── pom.xml                                     # Root aggregator POM
├── order-domain/                               # PURE JAVA 17 DOMAIN MODULE
│   ├── src/main/java/com/rabtech/order/domain/
│   │   ├── model/
│   │   │   ├── Order.java                      # Aggregate Root
│   │   │   ├── OrderId.java                    # Value Object
│   │   │   ├── OrderLine.java                  # Immutable Line Entity
│   │   │   ├── OrderLineId.java                # Value Object
│   │   │   ├── OrderStatus.java                # Enum: DRAFT, CONFIRMED, PAID, CANCELLED
│   │   │   ├── Money.java                      # Immutable Monetary Value Object
│   │   │   └── Quantity.java                   # Positive Whole Number Value Object
│   │   ├── events/
│   │   │   ├── DomainEvent.java                # Base Event Interface
│   │   │   ├── OrderConfirmed.java             # Record Event
│   │   │   ├── PaymentRecorded.java            # Record Event
│   │   │   └── OrderCancelled.java             # Record Event
│   │   ├── exceptions/
│   │   │   ├── OrderDomainException.java       # Base Exception
│   │   │   ├── InvalidOrderStateException.java
│   │   │   └── OrderValidationException.java
│   │   └── ports/
│   │       ├── OrderRepository.java            # Outbound Persistence Port
│   │       ├── NotificationPort.java           # Outbound Notification Port
│   │       └── TimeProvider.java               # Outbound Time Port
│   └── pom.xml
│
├── order-application/                          # USE CASES & APPLICATION SERVICE
│   ├── src/main/java/com/rabtech/order/application/
│   │   ├── commands/
│   │   │   ├── CreateOrderCommand.java
│   │   │   ├── AddOrderLineCommand.java
│   │   │   ├── RemoveOrderLineCommand.java
│   │   │   ├── ConfirmOrderCommand.java
│   │   │   ├── RecordPaymentCommand.java
│   │   │   └── CancelOrderCommand.java
│   │   └── service/
│   │       └── OrderApplicationService.java    # Orchestrates Domain with Ports
│   └── pom.xml
│
├── order-adapters/                             # IN-MEMORY & TEST ADAPTERS
│   ├── src/main/java/com/rabtech/order/adapters/
│   │   ├── inmemory/
│   │   │   ├── InMemoryOrderRepository.java    # Thread-safe in-memory store
│   │   │   └── RecordingNotificationAdapter.java # Event recorder for tests
│   │   └── time/
│   │       ├── SystemTimeProvider.java         # UTC system clock
│   │       └── TestTimeProvider.java           # Deterministic controllable clock
│   └── pom.xml
│
├── docs/adr/                                   # ARCHITECTURE DECISION RECORDS
│   ├── 0001-hexagonal-architecture-and-layer-boundaries.md
│   ├── 0002-order-aggregate-and-invariants.md
│   ├── 0003-value-objects-and-immutability.md
│   ├── 0004-state-machine-and-invalid-transitions.md
│   └── 0005-ports-and-in-memory-test-adapters.md
│
└── order-tests/                                # JUNIT 5 & ASSERTJ TEST SUITE
    ├── src/test/java/com/rabtech/order/
    │   ├── domain/
    │   │   ├── OrderAggregateTest.java         # Invariant enforcement & events (9 tests)
    │   │   ├── StateTransitionsTest.java       # Legal & illegal transition proofs (14 tests)
    │   │   └── ValueObjectsTest.java           # Value object invariants (11 tests)
    │   ├── application/
    │   │   └── OrderApplicationServiceTest.java # Workflow orchestration & ports (2 tests)
    │   └── architecture/
    │       └── DomainBoundaryTest.java         # Architecture boundary verification (1 test)
    └── pom.xml
4. Running the Tests
To compile and execute all 37 tests across the multi-module project, run:

Bash
mvn clean test
Test Results Summary
Plaintext
[INFO] Results:
[INFO] 
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Order Domain System 1.0.0-SNAPSHOT:
[INFO] 
[INFO] Order Domain System ................................ SUCCESS
[INFO] Order Domain ....................................... SUCCESS
[INFO] Order Application .................................. SUCCESS
[INFO] Order Adapters ..................................... SUCCESS
[INFO] Order Tests ........................................ SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
