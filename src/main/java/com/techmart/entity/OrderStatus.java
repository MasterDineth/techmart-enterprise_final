package com.techmart.entity;

/**
 * Lifecycle of an order as it moves through the asynchronous pipeline.
 */
public enum OrderStatus {
    /** Accepted by the web tier, queued for processing. */
    PENDING,
    /** Stock reserved, payment/fulfilment in progress (async worker). */
    PROCESSING,
    /** Successfully processed. */
    CONFIRMED,
    /** Could not be fulfilled (e.g. insufficient stock). */
    REJECTED
}
