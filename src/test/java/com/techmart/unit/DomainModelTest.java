package com.techmart.unit;

import com.techmart.dto.OrderLineRequest;
import com.techmart.dto.PlaceOrderRequest;
import com.techmart.dto.ProcessingResult;
import com.techmart.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    void orderItem_lineTotalCalculated() {
        Product p = new Product("SKU-1", "Widget", "Cat", new BigDecimal("10.00"), "desc");
        OrderItem item = new OrderItem(p, 3, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("30.00"), item.getLineTotal());
    }

    @Test
    void inventoryItem_availableIsQuantityMinusReserved() {
        Product p = new Product("SKU-2", "Gadget", "Cat", new BigDecimal("5.00"), "desc");
        Warehouse w = new Warehouse("WH-1", "Main", "NYC");
        InventoryItem item = new InventoryItem(p, w, 100);
        item.setReserved(30);
        assertEquals(70, item.getAvailable());
    }

    @Test
    void placeOrderRequest_linesAddedCorrectly() {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerName("Alice");
        req.setCustomerEmail("alice@example.com");
        OrderLineRequest line = new OrderLineRequest();
        line.setProductId(1L);
        line.setQuantity(2);
        req.getLines().add(line);

        assertEquals("Alice", req.getCustomerName());
        assertEquals(1, req.getLines().size());
        assertEquals(2, req.getLines().get(0).getQuantity());
    }

    @Test
    void processingResult_fieldsRoundtrip() {
        ProcessingResult r = new ProcessingResult(42L, "CONFIRMED", 150L, "ok");
        assertEquals(42L, r.getOrderId());
        assertEquals("CONFIRMED", r.getStatus());
        assertEquals(150L, r.getProcessingMillis());
        assertEquals("ok", r.getDetail());
    }

    @Test
    void orderEntity_addItemLinksBack() {
        OrderEntity order = new OrderEntity();
        Product p = new Product("SKU-3", "Thing", "Cat", new BigDecimal("20.00"), "desc");
        OrderItem item = new OrderItem(p, 1, new BigDecimal("20.00"));
        order.addItem(item);

        assertEquals(1, order.getItems().size());
        assertSame(order, item.getOrder());
    }

    @Test
    void orderStatus_allValuesPresent() {
        assertNotNull(OrderStatus.valueOf("PENDING"));
        assertNotNull(OrderStatus.valueOf("PROCESSING"));
        assertNotNull(OrderStatus.valueOf("CONFIRMED"));
        assertNotNull(OrderStatus.valueOf("REJECTED"));
    }
}
