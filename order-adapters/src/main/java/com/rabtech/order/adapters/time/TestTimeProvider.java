package com.rabtech.order.adapters.time;

import com.rabtech.order.domain.ports.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic, controllable time provider for tests.
 * Allows fixing, advancing, or rewinding time during unit and integration tests.
 */
public class TestTimeProvider implements TimeProvider {

    private final AtomicReference<Instant> currentInstant;

    public TestTimeProvider() {
        this(Instant.parse("2026-01-01T10:00:00Z"));
    }

    public TestTimeProvider(Instant initialInstant) {
        this.currentInstant = new AtomicReference<>(Objects.requireNonNull(initialInstant, "initialInstant cannot be null"));
    }

    @Override
    public Instant now() {
        return currentInstant.get();
    }

    public void setInstant(Instant newInstant) {
        this.currentInstant.set(Objects.requireNonNull(newInstant, "newInstant cannot be null"));
    }

    public void advance(Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        this.currentInstant.updateAndGet(instant -> instant.plus(duration));
    }
}
