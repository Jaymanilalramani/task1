package com.rabtech.order.adapters.inmemory;

import com.rabtech.order.domain.model.Order;
import com.rabtech.order.domain.model.OrderId;
import com.rabtech.order.domain.ports.OrderRepository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory adapter implementing the OrderRepository port.
 * Allows fast, isolated testing without database dependencies.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> database = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        database.put(order.id(), order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        Objects.requireNonNull(orderId, "OrderId cannot be null");
        return Optional.ofNullable(database.get(orderId));
    }

    @Override
    public boolean existsById(OrderId orderId) {
        Objects.requireNonNull(orderId, "OrderId cannot be null");
        return database.containsKey(orderId);
    }

    public int count() {
        return database.size();
    }

    public void clear() {
        database.clear();
    }
}
