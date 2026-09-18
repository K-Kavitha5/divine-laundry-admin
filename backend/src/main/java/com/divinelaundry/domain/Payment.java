package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 40)
    private String paymentNumber;

    @Column(name = "client_request_id", unique = true, length = 80)
    private String clientRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LaundryOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMode mode;

    @Column(name = "transaction_reference", length = 120)
    private String transactionReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    protected Payment() {}

    public Payment(
            String clientRequestId,
            LaundryOrder order,
            PaymentMode mode,
            String transactionReference,
            BigDecimal amount,
            Instant paidAt,
            String createdBy) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        this.clientRequestId = clientRequestId;
        this.order = order;
        this.mode = mode;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.paidAt = paidAt == null ? Instant.now() : paidAt;
        this.createdBy = createdBy;
    }

    public void assignPaymentNumber(String value) { this.paymentNumber = value; }

    public Long getId() { return id; }
    public String getPaymentNumber() { return paymentNumber; }
    public String getClientRequestId() { return clientRequestId; }
    public LaundryOrder getOrder() { return order; }
    public PaymentMode getMode() { return mode; }
    public String getTransactionReference() { return transactionReference; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
    public String getCreatedBy() { return createdBy; }
}
