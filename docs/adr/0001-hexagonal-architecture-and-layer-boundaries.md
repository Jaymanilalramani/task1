# ADR-001: Hexagonal Architecture and Layer Boundaries

## Status
Accepted

## Context
The Order domain has critical business rules governing an order's lifecycle, invariant checks, and domain events. A key requirement is:
> *"Keep the domain module independent of Spring, JPA, HTTP, and database adapters."*

Coupling domain logic to web frameworks (like Spring MVC/Web), persistence frameworks (such as JPA/Hibernate), or specific transport mechanisms creates tight coupling, slows down unit tests, forces database setup for testing business rules, and exposes domain models to database schema leaks (e.g. JPA getters/setters, default zero-arg constructors, mutable collections).

## Decision
We adopt **Hexagonal Architecture (Ports and Adapters)** with strict multi-module boundaries:

1. **`order-domain` (Core)**:
   - Contains pure business logic: Aggregate Root (`Order`), Entities (`OrderLine`), Value Objects (`OrderId`, `Money`, `Quantity`), Domain Events (`OrderConfirmed`, `PaymentRecorded`, `OrderCancelled`), and Domain Exceptions.
   - Defines outbound port interfaces (`OrderRepository`, `NotificationPort`, `TimeProvider`).
   - Zero runtime framework dependencies (built solely with standard Java 17 SE).

2. **`order-application` (Use Cases / Orchestration)**:
   - Contains application commands (`CreateOrderCommand`, `AddOrderLineCommand`, `ConfirmOrderCommand`, etc.) and the application service (`OrderApplicationService`).
   - Coordinates domain model operations with outbound ports.
   - Uses constructor-based dependency injection without framework annotations.

3. **`order-adapters` (Infrastructure / Adapters)**:
   - Provides concrete implementations of ports, specifically in-memory adapters (`InMemoryOrderRepository`, `RecordingNotificationAdapter`, `TestTimeProvider`).

4. **`order-tests` (Verification)**:
   - Exercises the entire domain, application services, and architecture boundary verification using JUnit 5 and AssertJ.

## Consequences
### Positive
- **Complete Framework Agnosticism**: Domain models and business rules can be migrated to any framework or run in CLI/AWS Lambda/batch jobs without code changes.
- **Fast & Deterministic Testing**: Tests execute entirely in memory in milliseconds without booting Spring contexts or configuring Testcontainers.
- **Clean Separation of Concerns**: Changes to database technology or notification mechanisms do not affect domain invariants.

### Negative / Trade-offs
- Requires mapping between domain models and persistence DTOs if external databases are added in the future.
- More boilerplate interfaces (ports) compared to anemic CRUD Spring Boot projects.
