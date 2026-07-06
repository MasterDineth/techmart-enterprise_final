package com.techmart.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.logging.Logger;

/**
 * Application-scoped bridge between the JMS notification topic and connected browsers
 */
@ApplicationScoped
public class NotificationBroadcaster {

    private static final Logger LOG = Logger.getLogger(NotificationBroadcaster.class.getName());

    private volatile Sse sse;
    private volatile SseBroadcaster broadcaster;

    /** Lazily wired from the SSE resource, which owns the {@link Sse} context. */
    public synchronized void init(Sse sse) {
        if (this.broadcaster == null) {
            this.sse = sse;
            this.broadcaster = sse.newBroadcaster();
            LOG.info("SSE broadcaster initialised.");
        }
    }

    public void register(SseEventSink sink) {
        if (broadcaster != null) {
            broadcaster.register(sink);
        }
    }

    public void broadcast(String eventName, String data) {
        if (broadcaster == null || sse == null) {
            return; // no subscribers yet
        }
        OutboundSseEvent event = sse.newEventBuilder()
                .name(eventName == null ? "notification" : eventName)
                .data(String.class, data)
                .build();
        broadcaster.broadcast(event);
    }
}
