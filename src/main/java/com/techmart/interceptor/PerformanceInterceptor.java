package com.techmart.interceptor;

import com.techmart.metrics.PerformanceMonitor;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Times every {@link Monitored} business call and feeds the result to the
 */
@Monitored
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class PerformanceInterceptor {

    @Inject
    PerformanceMonitor monitor;

    @AroundInvoke
    public Object measure(InvocationContext ctx) throws Exception {
        String op = ctx.getTarget().getClass().getSimpleName()
                .replace("$Proxy$_$$_WeldSubclass", "")   // strip Weld proxy suffix
                + "." + ctx.getMethod().getName();

        long start = System.nanoTime();
        boolean failed = false;
        try {
            return ctx.proceed();
        } catch (Exception e) {
            failed = true;
            throw e;   // preserve original exception semantics
        } finally {
            long micros = (System.nanoTime() - start) / 1000;
            monitor.record(op, micros, failed);
        }
    }
}
