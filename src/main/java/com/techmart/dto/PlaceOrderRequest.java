package com.techmart.dto;

import java.util.ArrayList;
import java.util.List;

/** Inbound checkout payload from the web UI. */
public class PlaceOrderRequest {
    private String customerName;
    private String customerEmail;
    private List<OrderLineRequest> lines = new ArrayList<>();

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public List<OrderLineRequest> getLines() { return lines; }
    public void setLines(List<OrderLineRequest> lines) { this.lines = lines; }
}
