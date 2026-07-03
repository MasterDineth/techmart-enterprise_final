package com.techmart.service;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when an order cannot be satisfied from available stock across all
 * warehouses. Marked {@code rollback = true} so the container rolls back the
 * reservation transaction, preventing the legacy overselling behaviour.
 */
@ApplicationException(rollback = true)
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
