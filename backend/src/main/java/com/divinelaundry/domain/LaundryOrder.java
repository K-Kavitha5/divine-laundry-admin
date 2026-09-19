package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "laundry_orders")
public class LaundryOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(name = "order_number", unique = true, length = 40)
    private String orderNumber;

    @Column(name = "invoice_number", unique = true, length = 40)
    private String invoiceNumber;

    @Column(name = "client_request_id", nullable = false, unique = true, length = 80)
    private String clientRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false, length = 30)
    private OrderStatus workStatus = OrderStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt = Instant.now();

    @Column(name = "pickup_at")
    private Instant pickupAt;

    @Column(name = "delivery_at")
    private Instant deliveryAt;

    @Column(length = 800)
    private String notes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "round_off", nullable = false, precision = 12, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected LaundryOrder() {}

    public LaundryOrder(String clientRequestId, Customer customer, Instant deliveryAt, String notes, String createdBy) {
        this(clientRequestId, customer, null, deliveryAt, notes, createdBy);
    }

    public LaundryOrder(String clientRequestId, Customer customer, Instant pickupAt, Instant deliveryAt,
            String notes, String createdBy) {
        this.clientRequestId = clientRequestId;
        this.customer = customer;
        this.pickupAt = pickupAt;
        this.deliveryAt = deliveryAt;
        this.notes = notes;
        this.createdBy = createdBy;
        validateSchedule(pickupAt, deliveryAt);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.attachTo(this);
    }

    public void calculateTotals(BigDecimal discount, BigDecimal tax) {
        this.subtotal = items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (discount.signum() < 0 || tax.signum() < 0 || discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount must be between zero and subtotal; tax cannot be negative");
        }
        this.discount = discount.max(BigDecimal.ZERO);
        this.tax = tax.max(BigDecimal.ZERO);
        BigDecimal exact = subtotal.subtract(this.discount).add(this.tax);
        BigDecimal rounded = exact.setScale(0, java.math.RoundingMode.HALF_UP).setScale(2);
        this.roundOff = rounded.subtract(exact);
        this.total = rounded;
        this.updatedAt = Instant.now();
    }

    public void assignOrderNumber(String value) { this.orderNumber = value; }

    public void finalizeInvoice(String value) {
        if (invoiceNumber == null) invoiceNumber = value;
    }

    public void changeStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (this.workStatus == status) {
            throw new IllegalStateException("Order is already in " + status + ".");
        }
        if (!OrderStatus.isValidTransition(this.workStatus, status)) {
            throw new IllegalStateException("Invalid order status transition: " + this.workStatus + " -> " + status);
        }
        this.workStatus = status;
        this.updatedAt = Instant.now();
    }

    public void recordPayment(BigDecimal amountPaid) {
        if (amountPaid == null || amountPaid.signum() <= 0) {
            this.paymentStatus = PaymentStatus.UNPAID;
        } else if (amountPaid.compareTo(total) < 0) {
            this.paymentStatus = PaymentStatus.PARTIAL;
        } else {
            this.paymentStatus = PaymentStatus.PAID;
        }
        this.updatedAt = Instant.now();
    }

    private static void validateSchedule(Instant pickupAt, Instant deliveryAt) {
        if (pickupAt != null && deliveryAt != null && pickupAt.isAfter(deliveryAt)) {
            throw new IllegalArgumentException("Pickup date and time cannot be after delivery date and time");
        }
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getClientRequestId() { return clientRequestId; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getWorkStatus() { return workStatus; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getPickupAt() { return pickupAt; }
    public Instant getDeliveryAt() { return deliveryAt; }
    public String getNotes() { return notes; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getRoundOff() { return roundOff; }
    public BigDecimal getTotal() { return total; }
    public String getCreatedBy() { return createdBy; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
