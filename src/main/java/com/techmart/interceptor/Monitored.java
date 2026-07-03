package com.techmart.interceptor;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor binding that switches on transparent performance monitoring.
 * Apply to a class or method and every invocation is timed and recorded in
 * {@link com.techmart.metrics.PerformanceMonitor}.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Monitored {
}
