package com.techmart.service;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when an order request is invalid (e.g. no order lines or an unknown product).
 */
@ApplicationException(rollback = true)
public class InvalidOrderException extends IllegalArgumentException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
