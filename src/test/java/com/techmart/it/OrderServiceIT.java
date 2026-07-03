package com.techmart.it;

import com.techmart.dto.OrderLineRequest;
import com.techmart.dto.PlaceOrderRequest;
import com.techmart.entity.OrderEntity;
import com.techmart.entity.OrderStatus;
import com.techmart.entity.Product;
import com.techmart.metrics.PerformanceMonitor;
import com.techmart.service.InsufficientStockException;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class OrderServiceIT {

    @Deployment
    static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test-order.war")
                .addPackages(true, "com.techmart")
                // The JMS destinations are provided by the target server (see
                // deploy/install.cli and the deployed techmart.war). Excluding
                // JmsConfig avoids re-declaring them, which would otherwise fail
                // deployment with a DuplicateServiceException for orderQueue.
                .deleteClass(com.techmart.jms.JmsConfig.class)
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private OrderService orderService;

    @EJB
    private ProductService productService;

    @Inject
    private PerformanceMonitor monitor;

    @Test
    void placeOrder_createsPendingOrder() {
        Product p = productService.findAll().get(0);
        PlaceOrderRequest req = buildRequest(p.getId(), 1);

        long before = monitor.getOrdersAccepted();
        OrderEntity order = orderService.placeOrder(req);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(1, order.getItems().size());
        assertTrue(order.getTotal().doubleValue() > 0);
        assertEquals(before + 1, monitor.getOrdersAccepted());
    }

    @Test
    void placeOrder_emptyLines_throwsIllegalArgument() {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerName("Test");
        req.setCustomerEmail("test@example.com");
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void placeOrder_unknownProduct_throwsIllegalArgument() {
        PlaceOrderRequest req = buildRequest(999999L, 1);
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void placeOrder_excessiveQuantity_throwsInsufficientStock() {
        Product p = productService.findAll().get(0);
        PlaceOrderRequest req = buildRequest(p.getId(), 999999);
        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(req));
    }

    @Test
    void findAll_includesPlacedOrder() {
        Product p = productService.findAll().get(1);
        orderService.placeOrder(buildRequest(p.getId(), 1));

        List<OrderEntity> orders = orderService.findAll();
        assertFalse(orders.isEmpty());
    }

    @Test
    void performanceMonitor_recordsMethodStats() {
        // Placing an order triggers @Monitored interceptor on OrderService
        Product p = productService.findAll().get(0);
        orderService.placeOrder(buildRequest(p.getId(), 1));

        boolean hasOrderStats = monitor.snapshot().stream()
                .anyMatch(s -> s.getName().contains("OrderService") || s.getName().contains("placeOrder"));
        assertTrue(hasOrderStats, "PerformanceInterceptor should have recorded OrderService stats");
    }

    private PlaceOrderRequest buildRequest(Long productId, int qty) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerName("IT Tester");
        req.setCustomerEmail("it-test@techmart.example");
        OrderLineRequest line = new OrderLineRequest();
        line.setProductId(productId);
        line.setQuantity(qty);
        req.setLines(List.of(line));
        return req;
    }
}
