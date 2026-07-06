package com.techmart.dto;

import java.io.Serializable;


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
