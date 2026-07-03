package com.techmart.rest;

import com.techmart.entity.Product;
import com.techmart.service.InventoryService;
import com.techmart.service.ProductService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @EJB
    private ProductService productService;

    @EJB
    private InventoryService inventoryService;

    @GET
    public List<Map<String, Object>> list() {
        return productService.findAll().stream().map(this::toView).toList();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> get(@PathParam("id") Long id) {
        Product p = productService.find(id);
        if (p == null) {
            throw new NotFoundException("Product " + id + " not found");
        }
        return toView(p);
    }

    private Map<String, Object> toView(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("sku", p.getSku());
        m.put("name", p.getName());
        m.put("category", p.getCategory());
        m.put("price", p.getPrice());
        m.put("available", inventoryService.availableFor(p.getId()));
        return m;
    }
}
