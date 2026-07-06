package com.techmart.metrics;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.annotation.PostConstruct;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Central, application-wide performance registry.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class PerformanceMonitor {

    private static final Logger LOG = Logger.getLogger(PerformanceMonitor.class.getName());

    private final Map<String, MethodStats> stats = new ConcurrentHashMap<>();

    // Domain-level throughput counters.
    private final AtomicLong ordersAccepted = new AtomicLong();
    private final AtomicLong ordersProcessed = new AtomicLong();
    private final AtomicLong jmsMessagesProduced = new AtomicLong();
    private final AtomicLong jmsMessagesConsumed = new AtomicLong();
    private final AtomicLong notificationsPushed = new AtomicLong();

    private volatile long startedAtMillis;

    @PostConstruct
    void init() {
        // Fixed epoch captured once at boot used to derive throughput/sec.
        startedAtMillis = System.currentTimeMillis();
        LOG.info("PerformanceMonitor singleton started - metrics collection active.");
    }

    /** Record one timed method invocation (micros = nanos/1000). */
    public void record(String operation, long micros, boolean failed) {
        stats.computeIfAbsent(operation, MethodStats::new).record(micros, failed);
    }

    public void incOrdersAccepted()      { ordersAccepted.incrementAndGet(); }
    public void incOrdersProcessed()     { ordersProcessed.incrementAndGet(); }
    public void incJmsProduced()         { jmsMessagesProduced.incrementAndGet(); }
    public void incJmsConsumed()         { jmsMessagesConsumed.incrementAndGet(); }
    public void incNotificationsPushed() { notificationsPushed.incrementAndGet(); }

    public List<MethodStats> snapshot() {
        return stats.values().stream()
                .sorted(Comparator.comparing(MethodStats::getName))
                .toList();
    }

    public long uptimeSeconds() {
        return Math.max(1, (System.currentTimeMillis() - startedAtMillis) / 1000);
    }

    public long getOrdersAccepted()      { return ordersAccepted.get(); }
    public long getOrdersProcessed()     { return ordersProcessed.get(); }
    public long getJmsMessagesProduced() { return jmsMessagesProduced.get(); }
    public long getJmsMessagesConsumed() { return jmsMessagesConsumed.get(); }
    public long getNotificationsPushed() { return notificationsPushed.get(); }

    /** Consumed-message throughput since boot. */
    public double getJmsThroughputPerSec() {
        return jmsMessagesConsumed.get() / (double) uptimeSeconds();
    }
}
