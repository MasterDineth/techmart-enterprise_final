package com.techmart.rest;

import com.techmart.dto.PlaceOrderRequest;
import com.techmart.entity.OrderEntity;
import com.techmart.entity.OrderItem;
import com.techmart.service.InsufficientStockException;
import com.techmart.service.OrderService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderService orderService;

    @POST
    public Response place(PlaceOrderRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Order must contain at least one line")).build();
        }
        try {
            OrderEntity order = orderService.placeOrder(request);
            return Response.status(Response.Status.CREATED).entity(toView(order)).build();
        } catch (InsufficientStockException e) {
            return Response.status(Response.Status.CONFLICT).entity(Map.of("error", e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    public List<Map<String, Object>> list() {
        return orderService.findAll().stream().map(this::toView).toList();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> get(@PathParam("id") Long id) {
        OrderEntity order = orderService.find(id);
        if (order == null) throw new NotFoundException("Order " + id + " not found");
        return toView(order);
    }

    private Map<String, Object> toView(OrderEntity o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("customer", o.getCustomer().getName());
        m.put("email", o.getCustomer().getEmail());
        m.put("status", o.getStatus());
        m.put("total", o.getTotal());
        m.put("createdAt", o.getCreatedAt().toString());
        m.put("items", o.getItems().stream().map(this::itemView).toList());
        return m;
    }

    private Map<String, Object> itemView(OrderItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", i.getProduct().getName());
        m.put("quantity", i.getQuantity());
        m.put("unitPrice", i.getUnitPrice());
        m.put("lineTotal", i.getLineTotal());
        return m;
    }
}
