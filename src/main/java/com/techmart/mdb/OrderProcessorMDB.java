package com.techmart.mdb;

import com.techmart.metrics.PerformanceMonitor;
import com.techmart.service.OrderService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-driven bean that fulfils queued orders asynchronously.
 *
 * <p>Lifecycle / throughput optimization: {@code maxSession = 20} lets WildFly
 * run a pool of 20 MDB instances consuming the queue in parallel, and
 * {@code Auto-acknowledge} keeps the ack path cheap. Each message is processed
 * in its own container transaction; an unhandled error triggers redelivery
 * (capped by the broker's max-delivery-attempts).</p>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/orderQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "20")
})
public class OrderProcessorMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(OrderProcessorMDB.class.getName());

    @EJB
    private OrderService orderService;

    @Inject
    private PerformanceMonitor monitor;

    @Override
    public void onMessage(Message message) {
        long start = System.nanoTime();
        Long orderId = null;
        try {
            MapMessage map = (MapMessage) message;
            orderId = map.getLong("orderId");

            orderService.fulfilOrder(orderId);

            monitor.incJmsConsumed();
            long micros = (System.nanoTime() - start) / 1000;
            monitor.record("OrderProcessorMDB.onMessage", micros, false);
            LOG.info("Fulfilled order " + orderId + " in " + (micros / 1000.0) + " ms");
        } catch (Exception e) {
            long micros = (System.nanoTime() - start) / 1000;
            monitor.record("OrderProcessorMDB.onMessage", micros, true);
            LOG.log(Level.SEVERE, "Failed to process order " + orderId + " - will redeliver", e);
            // Rethrow so the container rolls back and the broker redelivers.
            throw new RuntimeException("Order processing failed for " + orderId, e);
        }
    }
}
