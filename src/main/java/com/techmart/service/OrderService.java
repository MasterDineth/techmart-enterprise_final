package com.techmart.service;

import com.techmart.dto.OrderLineRequest;
import com.techmart.dto.OrderMessage;
import com.techmart.dto.PlaceOrderRequest;
import com.techmart.dto.ProcessingResult;
import com.techmart.entity.*;
import com.techmart.interceptor.Monitored;
import com.techmart.jms.NotificationPublisher;
import com.techmart.jms.OrderMessageProducer;
import com.techmart.metrics.PerformanceMonitor;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core order orchestration. Stateless so the container can pool instances and absorb peak-sale bursts.
 */
@Stateless
@Monitored
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class.getName());

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    @EJB private CustomerService customerService;
    @EJB private InventoryService inventoryService;
    @EJB private ProductService productService;
    @EJB private OrderMessageProducer orderProducer;
    @EJB private NotificationPublisher notificationPublisher;

    @Inject private PerformanceMonitor monitor;

    /**
     * Accept an order, reserve stock atomically, persist as PENDING, and queue it for asynchronous fulfilment.
     */
    public OrderEntity placeOrder(PlaceOrderRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one line");
        }

        Customer customer = customerService.findOrCreate(request.getCustomerName(), request.getCustomerEmail());

        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineRequest line : request.getLines()) {
            Product product = productService.find(line.getProductId());
            if (product == null) {
                throw new InvalidOrderException("Unknown product: " + line.getProductId());
            }
            // Reserve first - throws InsufficientStockException (rollback) if short.
            inventoryService.reserve(product.getId(), line.getQuantity());

            OrderItem item = new OrderItem(product, line.getQuantity(), product.getPrice());
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }
        order.setTotal(total);
        em.persist(order);
        em.flush(); // materialize the id for the JMS message

        // Hand off to the asynchronous pipeline.
        orderProducer.send(new OrderMessage(order.getId(), customer.getEmail()));
        monitor.incOrdersAccepted();

        LOG.info("Accepted order " + order.getId() + " for " + customer.getEmail() + " (queued for fulfilment)");
        return order;
    }

    /**
     * Confirm a queued order, mark as CONFIRMED, and send a notification to the customer.
     */
    public void fulfilOrder(Long orderId) {
        OrderEntity order = em.find(OrderEntity.class, orderId);
        if (order == null) {
            LOG.warning("fulfilOrder: order " + orderId + " not found");
            return;
        }
        order.setStatus(OrderStatus.PROCESSING);

        for (OrderItem item : order.getItems()) {
            inventoryService.commitReservation(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        monitor.incOrdersProcessed();

        notificationPublisher.publish("ORDER_CONFIRMED", order.getCustomer().getEmail(),
                "Your order #" + order.getId() + " (total $" + order.getTotal() + ") has been confirmed.");
    }

    /**
      * self-measured processing time.
     */
    @Asynchronous
    public Future<ProcessingResult> processOrderAsync(Long orderId) {
        long start = System.nanoTime();
        try {
            fulfilOrder(orderId);
            long millis = (System.nanoTime() - start) / 1_000_000;
            return new AsyncResult<>(new ProcessingResult(orderId, "CONFIRMED", millis, "ok"));
        } catch (RuntimeException ex) {
            long millis = (System.nanoTime() - start) / 1_000_000;
            LOG.log(Level.SEVERE, "Async fulfilment failed for order " + orderId, ex);
            markRejected(orderId);
            return new AsyncResult<>(new ProcessingResult(orderId, "REJECTED", millis, ex.getMessage()));
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void markRejected(Long orderId) {
        OrderEntity order = em.find(OrderEntity.class, orderId);
        if (order != null) {
            order.setStatus(OrderStatus.REJECTED);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrderEntity> findAll() {
        return em.createNamedQuery("Order.findAll", OrderEntity.class)
                .setMaxResults(100)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrderEntity find(Long id) {
        return em.find(OrderEntity.class, id);
    }
}
