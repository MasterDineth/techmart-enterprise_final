package com.techmart.dto;

import java.io.Serializable;

/**
 * Lightweight, serializable order handle carried on the JMS order queue.
 * Only the id is sent - the MDB re-reads the order in its own transaction,
 * keeping messages tiny for high throughput.
 */
public class OrderMessage implements Serializable {
    private Long orderId;
    private String customerEmail;

    public OrderMessage() {
    }

    public OrderMessage(Long orderId, String customerEmail) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
