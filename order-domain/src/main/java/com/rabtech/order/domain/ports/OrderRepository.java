package com.rabtech.order.domain.ports;

import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;

import java.util.Optional;

/**
 * Outbound Port for Order persistence.
 * Decouples the domain model from underlying storage mechanisms (e.g. SQL, NoSQL, In-Memory).
 */
public interface OrderRepository {

    /**
     * Persists the given Order aggregate.
     */
    void save(Order order);

    /**
     * Finds an Order by its unique OrderId.
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * Checks if an Order with the specified ID exists.
     */
    boolean existsById(OrderId orderId);
}
