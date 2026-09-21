package com.divinelaundry.domain;

public enum PaymentRequestStatus {
    CREATED,
    PENDING,
    PAID,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this == PAID || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
}
