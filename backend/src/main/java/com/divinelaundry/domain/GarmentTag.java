package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "garment_tags")
public class GarmentTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_number", nullable = false, unique = true, length = 60)
    private String tagNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LaundryOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "piece_sequence", nullable = false)
    private int pieceSequence;

    @Column(name = "print_count", nullable = false)
    private int printCount;

    @Column(name = "last_printed_at")
    private Instant lastPrintedAt;

    protected GarmentTag() {}

    public GarmentTag(String tagNumber, LaundryOrder order, OrderItem orderItem, int pieceSequence) {
        this.tagNumber = tagNumber;
        this.order = order;
        this.orderItem = orderItem;
        this.pieceSequence = pieceSequence;
    }

    public void markPrinted() {
        printCount++;
        lastPrintedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTagNumber() { return tagNumber; }
    public LaundryOrder getOrder() { return order; }
    public OrderItem getOrderItem() { return orderItem; }
    public int getPieceSequence() { return pieceSequence; }
    public int getPrintCount() { return printCount; }
    public Instant getLastPrintedAt() { return lastPrintedAt; }
}
