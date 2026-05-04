package com.ganaderia4.backend.dto;

public class NotificationOutboxDetailDTO extends NotificationOutboxSummaryDTO {

    private String payloadPreview;
    private int payloadSize;

    public String getPayloadPreview() {
        return payloadPreview;
    }

    public void setPayloadPreview(String payloadPreview) {
        this.payloadPreview = payloadPreview;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }
}
