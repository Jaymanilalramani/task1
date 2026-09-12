package com.rabtech.order.adapters.inmemory;

import com.rabtech.order.domain.events.OrderCancelled;
import com.rabtech.order.domain.events.OrderConfirmed;
import com.rabtech.order.domain.events.PaymentRecorded;
import com.rabtech.order.domain.ports.NotificationPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory test adapter for NotificationPort that records all sent notifications.
 * Provides helper methods for JUnit assertions.
 */
public class RecordingNotificationAdapter implements NotificationPort {

    private final List<OrderConfirmed> confirmedEvents = new CopyOnWriteArrayList<>();
    private final List<PaymentRecorded> paymentEvents = new CopyOnWriteArrayList<>();
    private final List<OrderCancelled> cancelledEvents = new CopyOnWriteArrayList<>();

    @Override
    public void notifyOrderConfirmed(OrderConfirmed event) {
        confirmedEvents.add(event);
    }

    @Override
    public void notifyPaymentRecorded(PaymentRecorded event) {
        paymentEvents.add(event);
    }

    @Override
    public void notifyOrderCancelled(OrderCancelled event) {
        cancelledEvents.add(event);
    }

    public List<OrderConfirmed> getConfirmedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(confirmedEvents));
    }

    public List<PaymentRecorded> getPaymentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(paymentEvents));
    }

    public List<OrderCancelled> getCancelledEvents() {
        return Collections.unmodifiableList(new ArrayList<>(cancelledEvents));
    }

    public int totalNotificationsCount() {
        return confirmedEvents.size() + paymentEvents.size() + cancelledEvents.size();
    }

    public void clear() {
        confirmedEvents.clear();
        paymentEvents.clear();
        cancelledEvents.clear();
    }
}
