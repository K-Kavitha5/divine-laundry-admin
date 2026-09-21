package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Service
public class WhatsappService {
    private final WhatsappMessageRepository messages;
    private final LaundryOrderRepository orders;
    private final DocumentService documents;
    private final InvoicePaymentImageService imageService;
    private final PdfInvoiceService pdfInvoices;
    private final PaymentReceiptService paymentReceipts;
    private final PdfReceiptService pdfReceipts;
    private final WhatsappCloudApiClient provider;
    private final WhatsappProviderProperties properties;
    private final WhatsappMessageClaimService claims;
    private final WhatsappMessagePersistenceService persistence;

    @Autowired
    public WhatsappService(
            WhatsappMessageRepository messages,
            LaundryOrderRepository orders,
            DocumentService documents,
            InvoicePaymentImageService imageService,
            PdfInvoiceService pdfInvoices,
            PaymentReceiptService paymentReceipts,
            PdfReceiptService pdfReceipts,
            WhatsappCloudApiClient provider,
            WhatsappProviderProperties properties,
            WhatsappMessageClaimService claims,
            WhatsappMessagePersistenceService persistence) {
        this.messages = messages;
        this.orders = orders;
        this.documents = documents;
        this.imageService = imageService;
        this.pdfInvoices = pdfInvoices;
        this.paymentReceipts = paymentReceipts;
        this.pdfReceipts = pdfReceipts;
        this.provider = provider;
        this.properties = properties;
        this.claims = claims;
        this.persistence = persistence;
    }

    public WhatsappMessage queueInvoice(String orderNumber) {
        LaundryOrder order = requiredInvoicedOrder(orderNumber);
        return deliver(order, "INVOICE_IMAGE:" + order.getInvoiceNumber());
    }

    public WhatsappMessage sendPaymentUpdate(String orderNumber, String paymentNumber) {
        LaundryOrder order = requiredInvoicedOrder(orderNumber);
        PaymentReceiptService.PaymentReceiptDocument receipt = paymentReceipts.document(orderNumber, paymentNumber);
        return deliverDocument(order, "RECEIPT_PDF:" + paymentNumber,
            pdfReceipts.render(receipt), paymentNumber + ".pdf",
            new WhatsappCloudApiClient.TemplateValues(
                receipt.customerName(), receipt.receiptNumber(), receipt.orderNumber(),
                receipt.orderTotal(), receipt.amountReceived(), receipt.remainingOutstanding()));
        }

    public WhatsappMessage queueInvoicePdf(String orderNumber) {
        LaundryOrder order = requiredInvoicedOrder(orderNumber);
        DocumentService.DocumentBundle document = documents.document(orderNumber);
        return deliverDocument(order, "INVOICE_PDF:" + order.getInvoiceNumber(),
            pdfInvoices.render(document), order.getInvoiceNumber() + ".pdf",
            new WhatsappCloudApiClient.TemplateValues(
                order.getCustomer().getName(), order.getInvoiceNumber(), order.getOrderNumber(),
                order.getTotal(), document.paymentSummary().amountPaid(), document.paymentSummary().balance()));
    }

    public WhatsappMessage retry(String orderNumber, Long messageId) {
        WhatsappMessage message = messages.findByIdAndOrder_OrderNumber(messageId, orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp message not found for this order"));
        if (message.getDeliveryStatus() != com.divinelaundry.domain.WhatsappDeliveryStatus.FAILED) {
            throw new IllegalStateException("Only failed WhatsApp messages can be retried");
        }
        String key = message.getDeduplicationKey();
        if (key.startsWith("INVOICE_IMAGE:")) return queueInvoice(orderNumber);
        if (key.startsWith("INVOICE_PDF:")) return queueInvoicePdf(orderNumber);
        if (key.startsWith("RECEIPT_PDF:")) return sendPaymentUpdate(orderNumber, key.substring("RECEIPT_PDF:".length()));
        throw new IllegalStateException("Unsupported WhatsApp message type");
    }

    private WhatsappMessage deliver(LaundryOrder order, String deduplicationKey) {
        String phone = order.getCustomer().getPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalStateException("Customer phone number is required for WhatsApp delivery");
        }
        WhatsappMessage message = getOrCreate(deduplicationKey,
            () -> new WhatsappMessage(deduplicationKey, order, phone, properties.templateName()));
        if (message.isDeliveredOrSent()) return message;

        if (!provider.isConfigured()) {
            message.waitingForProvider(provider.configurationMessage());
            return persistence.save(message);
        }

        java.util.Optional<WhatsappMessage> claimed = claims.claim(deduplicationKey);
        if (claimed.isEmpty()) {
            return messages.findByDeduplicationKey(deduplicationKey).orElse(message);
        }
        message = claimed.get();
        if (message.getDeliveryStatus() != com.divinelaundry.domain.WhatsappDeliveryStatus.PENDING) return message;
        try {
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
            message.markFailed(failureDescription(error));
        }
        return persistence.save(message);
    }

        private WhatsappMessage deliverDocument(LaundryOrder order, String deduplicationKey,
            byte[] pdf, String filename,
            WhatsappCloudApiClient.TemplateValues values) {
        WhatsappMessage message = getOrCreate(deduplicationKey,
            () -> new WhatsappMessage(deduplicationKey, order, order.getCustomer().getPhone(),
                properties.documentTemplateName(), "DOCUMENT"));
        if (message.isDeliveredOrSent()) return message;
        if (!provider.isDocumentConfigured()) {
            message.waitingForProvider(provider.configurationMessage());
            return persistence.save(message);
        }
        java.util.Optional<WhatsappMessage> claimed = claims.claim(deduplicationKey);
        if (claimed.isEmpty()) {
            return messages.findByDeduplicationKey(deduplicationKey).orElse(message);
        }
        message = claimed.get();
        if (message.getDeliveryStatus() != com.divinelaundry.domain.WhatsappDeliveryStatus.PENDING) return message;
        try {
            WhatsappCloudApiClient.DeliveryResult result = provider.sendInvoiceAndPaymentDocument(
                    order.getCustomer().getPhone(), pdf, filename, values);
            message.markSent(result.mediaId(), result.providerMessageId());
        } catch (RuntimeException error) {
            message.markFailed(failureDescription(error));
        }
        return persistence.save(message);
    }

    private WhatsappMessage getOrCreate(String deduplicationKey,
            java.util.function.Supplier<WhatsappMessage> factory) {
        return persistence.createIfAbsent(deduplicationKey, factory);
    }

    private static String failureDescription(RuntimeException error) {
        if (error instanceof WhatsappCloudApiClient.WhatsappProviderException providerError) {
            return providerError.classification().name();
        }
        return WhatsappFailureClassification.UNKNOWN_FAILURE.name();
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
