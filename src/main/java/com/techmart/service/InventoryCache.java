package com.techmart.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Singleton, container-managed-concurrency cache of available stock per product.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@AccessTimeout(value = 2000)
public class InventoryCache {

    private static final Logger LOG = Logger.getLogger(InventoryCache.class.getName());

    private final Map<Long, Integer> availabilityByProduct = new ConcurrentHashMap<>();

    @PostConstruct
    void onStart() {
        LOG.info("InventoryCache singleton initialised.");
    }

    @PreDestroy
    void onStop() {
        LOG.info("InventoryCache shutting down; clearing " + availabilityByProduct.size() + " entries.");
        availabilityByProduct.clear();
    }

    @Lock(LockType.READ)
    public Integer get(Long productId) {
        return availabilityByProduct.get(productId);
    }

    @Lock(LockType.WRITE)
    public void put(Long productId, int available) {
        availabilityByProduct.put(productId, available);
    }

    @Lock(LockType.WRITE)
    public void invalidate(Long productId) {
        availabilityByProduct.remove(productId);
    }

    @Lock(LockType.READ)
    public int size() {
        return availabilityByProduct.size();
    }
}
