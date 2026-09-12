package com.rabtech.order.domain.ports;

import com.rabtech.order.domain.events.OrderCancelled;
import com.rabtech.order.domain.events.OrderConfirmed;
import com.rabtech.order.domain.events.PaymentRecorded;

/**
 * Outbound Port for publishing notifications and domain events to external parties
 * (e.g. Email, SMS, Message Broker, Event Bus).
 */
public interface NotificationPort {

    /**
     * Dispatches notification for order confirmation.
     */
    void notifyOrderConfirmed(OrderConfirmed event);

    /**
     * Dispatches notification for payment recording.
     */
    void notifyPaymentRecorded(PaymentRecorded event);

    /**
     * Dispatches notification for order cancellation.
     */
    void notifyOrderCancelled(OrderCancelled event);
}
