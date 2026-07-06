package com.techmart.service;

import com.techmart.entity.InventoryItem;
import com.techmart.interceptor.Monitored;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.logging.Logger;

/**
 * Stateless bean owning all stock mutations across warehouses.
 */
@Stateless
@Monitored
public class InventoryService {

    private static final Logger LOG = Logger.getLogger(InventoryService.class.getName());
    private static final int MAX_RETRIES = 3;

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    @EJB
    private InventoryCache cache;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<InventoryItem> findAll() {
        return em.createNamedQuery("InventoryItem.all", InventoryItem.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int availableFor(Long productId) {
        return em.createNamedQuery("InventoryItem.byProduct", InventoryItem.class)
                .setParameter("productId", productId)
                .getResultList().stream()
                .mapToInt(InventoryItem::getAvailable)
                .sum();
    }

    /**
     * Reserve {@code quantity} units of a product, spreading the reservation
     * across warehouses as needed. Runs in a fresh transaction so a failure
     * rolls back cleanly without touching the caller's work.
     *
     * @throws InsufficientStockException if total availability is too low
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reserve(Long productId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                doReserve(productId, quantity);
                cache.invalidate(productId);
                return;
            } catch (jakarta.persistence.OptimisticLockException | jakarta.persistence.LockTimeoutException ex) {
                LOG.warning("Contended reservation for product " + productId
                        + " (attempt " + attempt + "/" + MAX_RETRIES + ") - retrying");
                em.clear();
            }
        }
        throw new InsufficientStockException(
                "Could not reserve " + quantity + " units of product " + productId + " under contention");
    }

    private void doReserve(Long productId, int quantity) {
        List<InventoryItem> items = em.createNamedQuery("InventoryItem.byProduct", InventoryItem.class)
                .setParameter("productId", productId)
                .getResultList();

        int totalAvailable = items.stream().mapToInt(InventoryItem::getAvailable).sum();
        if (totalAvailable < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId
                            + " (requested " + quantity + ", available " + totalAvailable + ")");
        }

        int remaining = quantity;
        for (InventoryItem item : items) {
            if (remaining <= 0) break;
            int take = Math.min(item.getAvailable(), remaining);
            if (take > 0) {
                item.setReserved(item.getReserved() + take);
                remaining -= take;
            }
        }
        em.flush(); // force the version check now so we can retry within the loop
    }

    /** Confirm a prior reservation by decrementing physical stock. */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void commitReservation(Long productId, int quantity) {
        List<InventoryItem> items = em.createNamedQuery("InventoryItem.byProduct", InventoryItem.class)
                .setParameter("productId", productId)
                .getResultList();
        int remaining = quantity;
        for (InventoryItem item : items) {
            if (remaining <= 0) break;
            int take = Math.min(item.getReserved(), remaining);
            item.setReserved(item.getReserved() - take);
            item.setQuantity(item.getQuantity() - take);
            remaining -= take;
        }
        cache.invalidate(productId);
    }

    public void restock(Long inventoryItemId, int delta) {
        InventoryItem item = em.find(InventoryItem.class, inventoryItemId);
        if (item != null) {
            item.setQuantity(item.getQuantity() + delta);
            cache.invalidate(item.getProduct().getId());
        }
    }
}
