package com.techmart.jms;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;

/**
 * Declares the JMS destinations programmatically so the application isself-contained
 */
@JMSDestinationDefinitions({
        @JMSDestinationDefinition(
                name = JmsConfig.ORDER_QUEUE,
                interfaceName = "jakarta.jms.Queue",
                destinationName = "orderQueue"),
        @JMSDestinationDefinition(
                name = JmsConfig.NOTIFICATION_TOPIC,
                interfaceName = "jakarta.jms.Topic",
                destinationName = "notificationTopic")
})
@Singleton
@Startup
public class JmsConfig {

    /** Order-processing point-to-point queue. */
    public static final String ORDER_QUEUE = "java:/jms/queue/orderQueue";

    /** Fan-out notification topic (customer + internal subscribers). */
    public static final String NOTIFICATION_TOPIC = "java:/jms/topic/notificationTopic";
}
