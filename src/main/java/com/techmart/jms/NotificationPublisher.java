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
 * Publishes application events to the notification topic.
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
