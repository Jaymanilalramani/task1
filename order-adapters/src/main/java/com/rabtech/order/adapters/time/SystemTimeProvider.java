package com.rabtech.order.adapters.time;

import com.rabtech.order.domain.ports.TimeProvider;

import java.time.Instant;

/**
 * Production adapter for TimeProvider returning current system time.
 */
public class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
