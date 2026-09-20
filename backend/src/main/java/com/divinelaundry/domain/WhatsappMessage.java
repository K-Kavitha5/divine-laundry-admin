package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "whatsapp_messages")
public class WhatsappMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 120)
    private String deduplicationKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LaundryOrder order;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "template_name", length = 120)
    private String templateName;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType = "IMAGE";

    @Column(name = "media_storage_key")
    private String mediaStorageKey;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private WhatsappDeliveryStatus deliveryStatus = WhatsappDeliveryStatus.WAITING_FOR_PROVIDER;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    protected WhatsappMessage() {}

    public WhatsappMessage(String deduplicationKey, LaundryOrder order, String recipientPhone) {
        this(deduplicationKey, order, recipientPhone, "invoice_image");
    }

    public WhatsappMessage(String deduplicationKey, LaundryOrder order, String recipientPhone, String templateName) {
        this(deduplicationKey, order, recipientPhone, templateName, "IMAGE");
    }

    public WhatsappMessage(String deduplicationKey, LaundryOrder order, String recipientPhone,
            String templateName, String mediaType) {
        this.deduplicationKey = deduplicationKey;
        this.order = order;
        this.recipientPhone = recipientPhone;
        this.templateName = templateName;
        this.mediaType = mediaType;
    }

    public void waitingForProvider(String reason) {
        this.deliveryStatus = WhatsappDeliveryStatus.WAITING_FOR_PROVIDER;
        this.lastError = abbreviate(reason);
    }

    public void markPending() {
        this.deliveryStatus = WhatsappDeliveryStatus.PENDING;
        this.attemptCount++;
        this.lastError = null;
    }

    public void markSent(String mediaId, String providerMessageId) {
        this.mediaStorageKey = mediaId;
        this.providerMessageId = providerMessageId;
        this.deliveryStatus = WhatsappDeliveryStatus.SENT;
        this.lastError = null;
        this.sentAt = Instant.now();
    }

    public void markFailed(String error) {
        this.deliveryStatus = WhatsappDeliveryStatus.FAILED;
        this.lastError = abbreviate(error);
    }

    public boolean isDeliveredOrSent() {
        return deliveryStatus == WhatsappDeliveryStatus.SENT
                || deliveryStatus == WhatsappDeliveryStatus.DELIVERED;
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) return "Unknown WhatsApp provider error";
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }

    public Long getId() { return id; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public LaundryOrder getOrder() { return order; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getTemplateName() { return templateName; }
    public String getMediaType() { return mediaType; }
    public String getMediaStorageKey() { return mediaStorageKey; }
    public String getProviderMessageId() { return providerMessageId; }
    public WhatsappDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
