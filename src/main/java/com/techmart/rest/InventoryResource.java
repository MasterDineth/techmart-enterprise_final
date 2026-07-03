package com.techmart.rest;

import com.techmart.entity.InventoryItem;
import com.techmart.service.InventoryService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private InventoryService inventoryService;

    @GET
    public List<Map<String, Object>> list() {
        return inventoryService.findAll().stream().map(this::toView).toList();
    }

    private Map<String, Object> toView(InventoryItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("product", i.getProduct().getName());
        m.put("sku", i.getProduct().getSku());
        m.put("warehouse", i.getWarehouse().getCode());
        m.put("quantity", i.getQuantity());
        m.put("reserved", i.getReserved());
        m.put("available", i.getAvailable());
        return m;
    }
}
