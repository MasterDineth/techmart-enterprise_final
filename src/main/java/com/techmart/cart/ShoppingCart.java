package com.techmart.cart;

import com.techmart.dto.OrderLineRequest;
import com.techmart.dto.PlaceOrderRequest;
import com.techmart.entity.OrderEntity;
import com.techmart.interceptor.Monitored;
import com.techmart.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;


@Stateful
@StatefulTimeout(value = 30, unit = java.util.concurrent.TimeUnit.MINUTES)
@Monitored
public class ShoppingCart implements Serializable {

    private static final Logger LOG = Logger.getLogger(ShoppingCart.class.getName());

    // productId -> quantity
    private final Map<Long, Integer> lines = new LinkedHashMap<>();

    @EJB
    private transient OrderService orderService;

    public void add(Long productId, int quantity) {
        lines.merge(productId, quantity, Integer::sum);
    }

    public void setQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            lines.remove(productId);
        } else {
            lines.put(productId, quantity);
        }
    }

    public void remove(Long productId) {
        lines.remove(productId);
    }

    public Map<Long, Integer> getLines() {
        return new LinkedHashMap<>(lines);
    }

    public int getItemCount() {
        return lines.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void clear() {
        lines.clear();
    }

    /**
     * Convert the cart to an order and end this stateful conversation.
     */
    @Remove
    public OrderEntity checkout(String customerName, String customerEmail) {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerEmail);
        lines.forEach((productId, qty) -> {
            OrderLineRequest line = new OrderLineRequest();
            line.setProductId(productId);
            line.setQuantity(qty);
            request.getLines().add(line);
        });
        OrderEntity order = orderService.placeOrder(request);
        lines.clear();
        return order;
    }

    @PostConstruct
    void created() {
        LOG.fine("ShoppingCart created.");
    }

    @PrePassivate
    void passivating() {
        LOG.fine("ShoppingCart passivating (" + lines.size() + " lines).");
    }

    @PostActivate
    void activated() {
        LOG.fine("ShoppingCart activated (" + lines.size() + " lines).");
    }

    @PreDestroy
    void destroyed() {
        LOG.fine("ShoppingCart destroyed.");
    }
}
