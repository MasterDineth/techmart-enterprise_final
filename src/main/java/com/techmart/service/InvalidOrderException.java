package com.techmart.service;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when an order request is invalid (e.g. no order lines or an unknown
 * product). Marked as an {@code @ApplicationException} with {@code rollback =
 * true} so the container rolls back the transaction and propagates the
 * exception to the caller unwrapped, instead of masking it inside an
 * {@link jakarta.ejb.EJBException}.
 *
 * <p>Extends {@link IllegalArgumentException} to preserve the existing
 * contract that order validation failures surface as illegal arguments.
 */
@ApplicationException(rollback = true)
public class InvalidOrderException extends IllegalArgumentException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
