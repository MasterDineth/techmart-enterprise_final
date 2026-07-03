package com.techmart.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Singleton, container-managed-concurrency cache of available stock per product.
 *
 * <p>Demonstrates EJB read/write locking: reads use {@link LockType#READ} so
 * many threads hit the hot path in parallel, while invalidation takes a brief
 * {@link LockType#WRITE} lock. This shields MySQL from the read storm generated
 * by 10,000 concurrent shoppers browsing availability.</p>
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
