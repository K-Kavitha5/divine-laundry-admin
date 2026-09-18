package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappService {
    private final WhatsappMessageRepository messages;
    private final LaundryOrderRepository orders;
    private final DocumentService documents;
    private final InvoicePaymentImageService imageService;
    private final WhatsappCloudApiClient provider;
    private final WhatsappProviderProperties properties;

    public WhatsappService(
            WhatsappMessageRepository messages,
            LaundryOrderRepository orders,
            DocumentService documents,
            InvoicePaymentImageService imageService,
            WhatsappCloudApiClient provider,
            WhatsappProviderProperties properties) {
        this.messages = messages;
        this.orders = orders;
        this.documents = documents;
        this.imageService = imageService;
        this.provider = provider;
        this.properties = properties;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public WhatsappMessage queueInvoice(String orderNumber) {
        LaundryOrder order = requiredInvoicedOrder(orderNumber);
        return deliver(order, "INVOICE_IMAGE:" + order.getInvoiceNumber());
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public WhatsappMessage sendPaymentUpdate(String orderNumber, String paymentNumber) {
        LaundryOrder order = requiredInvoicedOrder(orderNumber);
        return deliver(order, "PAYMENT_IMAGE:" + paymentNumber);
    }

    private WhatsappMessage deliver(LaundryOrder order, String deduplicationKey) {
        String phone = order.getCustomer().getPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalStateException("Customer phone number is required for WhatsApp delivery");
        }
        WhatsappMessage message = messages.findByDeduplicationKey(deduplicationKey)
                .orElseGet(() -> new WhatsappMessage(
                        deduplicationKey, order, phone, properties.templateName()));
        if (message.isDeliveredOrSent()) return message;

        if (!provider.isConfigured()) {
            message.waitingForProvider(provider.configurationMessage());
            return messages.save(message);
        }

        try {
            message.markPending();
            messages.saveAndFlush(message);
            DocumentService.DocumentBundle bundle = documents.document(order.getOrderNumber());
            byte[] png = imageService.render(bundle);
            WhatsappCloudApiClient.DeliveryResult result = provider.sendInvoiceAndPaymentImage(
                    phone,
                    png,
                    order.getInvoiceNumber() + ".png",
                    new WhatsappCloudApiClient.TemplateValues(
                            order.getCustomer().getName(),
                            order.getInvoiceNumber(),
                            order.getOrderNumber(),
                            order.getTotal(),
                            bundle.paymentSummary().amountPaid(),
                            bundle.paymentSummary().balance()));
            message.markSent(result.mediaId(), result.providerMessageId());
        } catch (RuntimeException error) {
            message.markFailed(error.getMessage());
        }
        return messages.save(message);
    }

    private LaundryOrder requiredInvoicedOrder(String orderNumber) {
        LaundryOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getInvoiceNumber() == null) {
            throw new IllegalStateException("Create the invoice before WhatsApp delivery");
        }
        return order;
    }
}
