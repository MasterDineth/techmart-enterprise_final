package com.techmart.mdb;

import com.techmart.entity.NotificationLog;
import com.techmart.metrics.PerformanceMonitor;
import com.techmart.rest.NotificationBroadcaster;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.logging.Level;
import java.util.logging.Logger;


@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/topic/notificationTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10")
})
public class NotificationMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NotificationMDB.class.getName());

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    @Inject
    private NotificationBroadcaster broadcaster;

    @Inject
    private PerformanceMonitor monitor;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        long start = System.nanoTime();
        try {
            TextMessage text = (TextMessage) message;
            String body = text.getText();
            String type = text.getStringProperty("type");
            String recipient = text.getStringProperty("recipient");

            // Durable audit trail.
            em.persist(new NotificationLog(type, "SSE", recipient, body));

            // Real-time push to the browser dashboard.
            broadcaster.broadcast(type, body);

            monitor.incJmsConsumed();
            monitor.incNotificationsPushed();
            monitor.record("NotificationMDB.onMessage", (System.nanoTime() - start) / 1000, false);
        } catch (Exception e) {
            monitor.record("NotificationMDB.onMessage", (System.nanoTime() - start) / 1000, true);
            LOG.log(Level.WARNING, "Failed to handle notification message", e);
        }
    }
}
