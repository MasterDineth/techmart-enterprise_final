package com.techmart.rest;

import com.techmart.metrics.MethodStats;
import com.techmart.metrics.PerformanceMonitor;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

    @Inject
    private PerformanceMonitor monitor;

    @GET
    public Map<String, Object> get() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uptimeSeconds", monitor.uptimeSeconds());
        m.put("ordersAccepted", monitor.getOrdersAccepted());
        m.put("ordersProcessed", monitor.getOrdersProcessed());
        m.put("jmsProduced", monitor.getJmsMessagesProduced());
        m.put("jmsConsumed", monitor.getJmsMessagesConsumed());
        m.put("notificationsPushed", monitor.getNotificationsPushed());
        m.put("jmsThroughputPerSec", monitor.getJmsThroughputPerSec());
        m.put("methods", monitor.snapshot().stream().map(this::statsView).toList());
        return m;
    }

    private Map<String, Object> statsView(MethodStats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", s.getName());
        m.put("invocations", s.getInvocations());
        m.put("errors", s.getErrors());
        m.put("avgMs", round(s.getAvgMillis()));
        m.put("minMs", round(s.getMinMillis()));
        m.put("maxMs", round(s.getMaxMillis()));
        return m;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
