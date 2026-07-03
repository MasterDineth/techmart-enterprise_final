package com.techmart.jms;

import com.techmart.interceptor.Monitored;
import com.techmart.metrics.PerformanceMonitor;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;

/**
 * Publishes application events to the notification topic. Every interested
 * subscriber (the real-time SSE bridge, audit logger, future e-mail/SMS
 * gateways) receives its own copy.
 *
 * <p>Notifications use {@code NON_PERSISTENT} delivery with a short time-to-live:
 * they are latency-sensitive and disposable, so we trade durability for
 * throughput.</p>
 */
@Stateless
@Monitored
public class NotificationPublisher {

    @Inject
    private JMSContext context;

    @Resource(lookup = JmsConfig.NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    @Inject
    private PerformanceMonitor monitor;

    public void publish(String type, String recipient, String body) {
        TextMessage message = context.createTextMessage(body);
        try {
            message.setStringProperty("type", type);
            message.setStringProperty("recipient", recipient == null ? "" : recipient);
        } catch (jakarta.jms.JMSException e) {
            throw new jakarta.jms.JMSRuntimeException(e.getMessage());
        }
        context.createProducer()
                .setDeliveryMode(DeliveryMode.NON_PERSISTENT)
                .setTimeToLive(60_000)   // notifications are stale after 60s
                .send(notificationTopic, message);
        monitor.incJmsProduced();
    }
}
