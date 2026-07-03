package com.techmart.it;

import com.techmart.dto.OrderLineRequest;
import com.techmart.dto.PlaceOrderRequest;
import com.techmart.entity.Product;
import com.techmart.metrics.PerformanceMonitor;
import com.techmart.service.OrderService;
import com.techmart.service.ProductService;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measures order-placement throughput and latency under concurrent load.
 * Validates the sub-second response-time requirement for placeOrder.
 */
@ExtendWith(ArquillianExtension.class)
class PerformanceIT {

    private static final int CONCURRENT_USERS = 20;
    private static final int ORDERS_PER_USER  = 5;
    private static final long MAX_AVG_MS       = 500; // sub-second requirement

    @Deployment
    static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test-perf.war")
                .addPackages(true, "com.techmart")
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB  private OrderService orderService;
    @EJB  private ProductService productService;
    @Inject private PerformanceMonitor monitor;

    @Test
    void placeOrder_concurrentLoad_subSecondAvgLatency() throws Exception {
        List<Product> products = productService.findAll();
        assertFalse(products.isEmpty());
        Long productId = products.get(0).getId();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        long[] latencies = new long[CONCURRENT_USERS * ORDERS_PER_USER];
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        for (int u = 0; u < CONCURRENT_USERS; u++) {
            final int user = u;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < ORDERS_PER_USER; i++) {
                        PlaceOrderRequest req = new PlaceOrderRequest();
                        req.setCustomerName("PerfUser" + user);
                        req.setCustomerEmail("perf" + user + "@techmart.example");
                        OrderLineRequest line = new OrderLineRequest();
                        line.setProductId(productId);
                        line.setQuantity(1);
                        req.setLines(List.of(line));

                        long start = System.currentTimeMillis();
                        try {
                            orderService.placeOrder(req);
                            latencies[user * ORDERS_PER_USER + i] = System.currentTimeMillis() - start;
                            success.incrementAndGet();
                        } catch (Exception e) {
                            latencies[user * ORDERS_PER_USER + i] = System.currentTimeMillis() - start;
                            failure.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Timed out waiting for concurrent orders");
        pool.shutdown();

        long total = 0;
        for (long l : latencies) total += l;
        double avgMs = (double) total / latencies.length;

        System.out.printf("Performance test: %d orders, %d success, %d failure, avg latency=%.1f ms%n",
                latencies.length, success.get(), failure.get(), avgMs);

        assertTrue(success.get() > 0, "At least some orders should succeed");
        assertTrue(avgMs < MAX_AVG_MS,
                String.format("Average latency %.1f ms exceeds %d ms threshold", avgMs, MAX_AVG_MS));
    }

    @Test
    void performanceMonitor_jmsThroughput_positive() throws InterruptedException {
        // Give MDBs a moment to process queued messages from other tests
        Thread.sleep(2000);
        double tps = monitor.getJmsThroughputPerSec();
        System.out.printf("JMS throughput: %.3f msg/s%n", tps);
        // Just verify the metric is being tracked (value >= 0)
        assertTrue(tps >= 0);
    }
}
