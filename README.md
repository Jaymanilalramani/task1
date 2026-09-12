# Order Domain – Clean Architecture

## Overview

This project implements an **Order Management Domain** using Java 17 and Clean/Hexagonal Architecture principles.

The main goal is to keep the business logic independent from frameworks, databases, and external technologies.

## Project Structure

```text
order-domain/
├── order-domain/
├── order-application/
├── order-adapters/
├── order-tests/
├── docs/
│   └── adr/
├── pom.xml
├── README.md
├── run-app.bat
├── run-app.ps1
├── run-tests.bat
└── run-tests.ps1
```

## Modules

### 1. Order Domain

Contains the core business logic:

* Order entity
* Value Objects
* Commands
* Domain Events
* Business rules

### 2. Order Application

Contains application-level use cases and services.

### 3. Order Adapters

Contains implementations of application ports such as in-memory repositories and external adapters.

### 4. Order Tests

Contains unit and integration tests for validating the application and domain logic.

## Technologies Used

* Java 17
* Maven
* JUnit 5
* Git & GitHub
* Clean Architecture
* Hexagonal Architecture
* Domain-Driven Design principles

## Architecture

The project follows a layered architecture:

```text
        Application
             |
             v
      Domain / Business Logic
             |
             v
          Ports
             |
             v
          Adapters
```

The domain layer does not depend on external frameworks or infrastructure.

## Key Features

* Pure Java domain model
* Order management workflow
* Domain entities and value objects
* Commands and domain events
* Ports and adapters
* In-memory implementation
* Automated tests
* Architecture Decision Records (ADRs)

## Testing

Tests can be executed using Maven:

```bash
mvn test
```

## Running the Project

Build the complete project:

```bash
mvn clean install
```

Run the application using the provided scripts:

```text
run-app.bat
```

or

```text
run-app.ps1
```

## Architecture Decision Records

Important architectural decisions are documented inside:

```text
docs/adr/
```

These documents explain the major design and architecture choices made during development.

## Author

**Jay Ramani**

Computer Engineering Student

## License

This project is developed for educational and internship purposes.
