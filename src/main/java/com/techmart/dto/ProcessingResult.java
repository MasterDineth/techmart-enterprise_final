package com.techmart.dto;

/** Outcome of asynchronous order fulfilment, returned via a {@code Future}. */
public class ProcessingResult {
    private Long orderId;
    private String status;
    private long processingMillis;
    private String detail;

    public ProcessingResult() {
    }

    public ProcessingResult(Long orderId, String status, long processingMillis, String detail) {
        this.orderId = orderId;
        this.status = status;
        this.processingMillis = processingMillis;
        this.detail = detail;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getProcessingMillis() { return processingMillis; }
    public void setProcessingMillis(long processingMillis) { this.processingMillis = processingMillis; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
