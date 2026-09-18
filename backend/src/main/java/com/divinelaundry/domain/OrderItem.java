package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LaundryOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private LaundryServiceItem service;

    @Column(name = "service_code", nullable = false, length = 40)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 160)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false, length = 20)
    private PricingUnit pricingUnit;

    @Column(name = "unit_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitRate;

    @Column(name = "billable_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal billableQuantity;

    @Column(name = "piece_count", nullable = false)
    private int pieceCount;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "no_print", nullable = false)
    private boolean noPrint;

    protected OrderItem() {}

    public OrderItem(LaundryServiceItem service, BigDecimal billableQuantity, int pieceCount, boolean noPrint) {
        if (billableQuantity.signum() <= 0) throw new IllegalArgumentException("Billable quantity must be positive");
        if (pieceCount < 0) throw new IllegalArgumentException("Piece count cannot be negative");
        this.service = service;
        this.serviceCode = service.getCode();
        this.serviceName = service.getName();
        this.pricingUnit = service.getPricingUnit();
        this.unitRate = service.getUnitRate();
        this.billableQuantity = billableQuantity;
        this.pieceCount = pieceCount;
        this.noPrint = noPrint;
        this.lineTotal = unitRate.multiply(billableQuantity).setScale(2, RoundingMode.HALF_UP);
    }

    void attachTo(LaundryOrder order) { this.order = order; }

    public Long getId() { return id; }
    public String getServiceCode() { return serviceCode; }
    public String getServiceName() { return serviceName; }
    public PricingUnit getPricingUnit() { return pricingUnit; }
    public BigDecimal getUnitRate() { return unitRate; }
    public BigDecimal getBillableQuantity() { return billableQuantity; }
    public int getPieceCount() { return pieceCount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public boolean isNoPrint() { return noPrint; }
}
