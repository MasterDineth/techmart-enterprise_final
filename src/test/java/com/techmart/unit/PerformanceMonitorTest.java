package com.techmart.unit;

import com.techmart.metrics.MethodStats;
import com.techmart.metrics.PerformanceMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMonitorTest {

    @Test
    void methodStats_recordsCorrectly() {
        MethodStats s = new MethodStats("test.op");
        s.record(1000, false);
        s.record(3000, false);
        s.record(2000, true);

        assertEquals(3, s.getInvocations());
        assertEquals(1, s.getErrors());
        assertEquals(1.0, s.getMinMillis(), 0.01);
        assertEquals(3.0, s.getMaxMillis(), 0.01);
        assertEquals(2.0, s.getAvgMillis(), 0.01);
    }

    @Test
    void methodStats_noInvocations_returnsZero() {
        MethodStats s = new MethodStats("empty");
        assertEquals(0.0, s.getAvgMillis());
        assertEquals(0.0, s.getMinMillis());
        assertEquals(0.0, s.getMaxMillis());
    }

    @Test
    void performanceMonitor_countersIncrement() {
        PerformanceMonitor m = new PerformanceMonitor();
        m.incOrdersAccepted();
        m.incOrdersAccepted();
        m.incOrdersProcessed();
        m.incJmsProduced();
        m.incJmsConsumed();
        m.incNotificationsPushed();

        assertEquals(2, m.getOrdersAccepted());
        assertEquals(1, m.getOrdersProcessed());
        assertEquals(1, m.getJmsMessagesProduced());
        assertEquals(1, m.getJmsMessagesConsumed());
        assertEquals(1, m.getNotificationsPushed());
    }

    @Test
    void performanceMonitor_snapshotSortedByName() {
        PerformanceMonitor m = new PerformanceMonitor();
        m.record("z.method", 100, false);
        m.record("a.method", 200, false);
        m.record("m.method", 150, false);

        var snapshot = m.snapshot();
        assertEquals(3, snapshot.size());
        assertEquals("a.method", snapshot.get(0).getName());
        assertEquals("m.method", snapshot.get(1).getName());
        assertEquals("z.method", snapshot.get(2).getName());
    }
}
