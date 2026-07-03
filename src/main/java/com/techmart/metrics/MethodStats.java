package com.techmart.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe rolling statistics for a single monitored operation.
 * All timings are stored in microseconds.
 */
public class MethodStats {

    private final String name;
    private final AtomicLong invocations = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong totalMicros = new AtomicLong();
    private final AtomicLong maxMicros = new AtomicLong();
    private final AtomicLong minMicros = new AtomicLong(Long.MAX_VALUE);

    public MethodStats(String name) {
        this.name = name;
    }

    public void record(long micros, boolean failed) {
        invocations.incrementAndGet();
        if (failed) {
            errors.incrementAndGet();
        }
        totalMicros.addAndGet(micros);
        maxMicros.accumulateAndGet(micros, Math::max);
        minMicros.accumulateAndGet(micros, Math::min);
    }

    public String getName() { return name; }
    public long getInvocations() { return invocations.get(); }
    public long getErrors() { return errors.get(); }

    public double getAvgMillis() {
        long n = invocations.get();
        return n == 0 ? 0.0 : (totalMicros.get() / (double) n) / 1000.0;
    }

    public double getMaxMillis() {
        return maxMicros.get() / 1000.0;
    }

    public double getMinMillis() {
        long m = minMicros.get();
        return m == Long.MAX_VALUE ? 0.0 : m / 1000.0;
    }
}
