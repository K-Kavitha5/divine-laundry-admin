package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_requests",
        indexes = {
                @Index(name = "idx_payment_requests_order_status", columnList = "order_id, status"),
                @Index(name = "idx_payment_requests_idempotency_key", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_payment_requests_provider_reference", columnList = "provider_reference", unique = true),
                @Index(name = "idx_payment_requests_provider_payment_id", columnList = "provider_payment_id", unique = true)
        })
public class PaymentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private LaundryOrder order;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, length = 12)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentRequestStatus status = PaymentRequestStatus.CREATED;

    @Column(length = 80)
    private String provider;

    @Column(name = "provider_reference", length = 160)
    private String providerReference;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "qr_payload", length = 2000)
    private String qrPayload;

    @Column(name = "provider_payment_id", length = 160)
    private String providerPaymentId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    protected PaymentRequest() {}

    public PaymentRequest(
            LaundryOrder order,
            BigDecimal requestedAmount,
            String currency,
            String provider,
            String createdBy,
            String idempotencyKey) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            throw new IllegalArgumentException("Requested amount must be greater than zero");
        }
        if (requestedAmount.scale() > 2) {
            throw new IllegalArgumentException("Requested amount cannot have more than two decimal places");
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("Created by is required");
        }
        this.order = order;
        this.orderNumber = order.getOrderNumber();
        this.requestedAmount = requestedAmount;
        this.currency = currency;
        this.provider = provider;
        this.createdBy = createdBy;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentRequestStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public void markPending(String providerReference, Instant expiresAt) {
        if (providerReference == null || providerReference.isBlank()) {
            throw new IllegalArgumentException("Provider reference is required");
        }
        this.providerReference = providerReference;
        this.expiresAt = expiresAt;
        this.status = PaymentRequestStatus.PENDING;
    }

    public void markPaid(String providerPaymentId) {
        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            throw new IllegalArgumentException("Provider payment ID is required");
        }
        this.providerPaymentId = providerPaymentId;
        this.paidAt = Instant.now();
        this.status = PaymentRequestStatus.PAID;
    }

    public void markFailed(String reason) {
        this.failureReason = reason;
        this.status = PaymentRequestStatus.FAILED;
        this.expiresAt = Instant.now();
    }

    public void resetForRetry() {
        this.providerReference = null;
        this.paymentUrl = null;
        this.qrPayload = null;
        this.providerPaymentId = null;
        this.failureReason = null;
        this.expiresAt = null;
        this.status = PaymentRequestStatus.CREATED;
    }

    public void markExpired() {
        this.status = PaymentRequestStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = PaymentRequestStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public LaundryOrder getOrder() { return order; }
    public String getOrderNumber() { return orderNumber; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public String getCurrency() { return currency; }
    public PaymentRequestStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getQrPayload() { return qrPayload; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getCreatedBy() { return createdBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailureReason() { return failureReason; }

    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setStatus(PaymentRequestStatus status) { this.status = status; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
}
