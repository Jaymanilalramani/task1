package com.rabtech.order.domain.ports;

import java.time.Instant;

/**
 * Outbound Port for abstracting system time.
 * Decouples domain and application logic from System.currentTimeMillis() or Instant.now(),
 * allowing deterministic time simulation in test suites.
 */
@FunctionalInterface
public interface TimeProvider {

    /**
     * Returns the current instant according to this provider.
     */
    Instant now();
}
