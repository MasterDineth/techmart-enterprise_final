package com.techmart.jms;

import com.techmart.dto.OrderMessage;
import com.techmart.interceptor.Monitored;
import com.techmart.metrics.PerformanceMonitor;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.*;

import java.util.logging.Logger;

/**
 * Sends accepted orders onto the JMS order queue.
 */
@Stateless
@Monitored
public class OrderMessageProducer {

    private static final Logger LOG = Logger.getLogger(OrderMessageProducer.class.getName());

    @Inject
    private JMSContext context;

    @Resource(lookup = JmsConfig.ORDER_QUEUE)
    private Queue orderQueue;

    @Inject
    private PerformanceMonitor monitor;

    public void send(OrderMessage order) {
        try {
            MapMessage message = context.createMapMessage();
            message.setLong("orderId", order.getOrderId());
            message.setString("customerEmail", order.getCustomerEmail());

            context.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setPriority(6)
                    .send(orderQueue, message);

            monitor.incJmsProduced();
        } catch (JMSException e) {
            // wrap checked calls.
            throw new JMSRuntimeException("Failed to enqueue order " + order.getOrderId() + ": " + e.getMessage());
        }
    }
}
