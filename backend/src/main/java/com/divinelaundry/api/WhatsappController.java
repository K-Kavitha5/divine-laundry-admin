package com.divinelaundry.api;

import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.service.WhatsappService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/notifications/whatsapp")
public class WhatsappController {
    private final WhatsappService whatsapp;

    public WhatsappController(WhatsappService whatsapp) {
        this.whatsapp = whatsapp;
    }

    @PostMapping("/invoice/{orderNumber}/queue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    WhatsappQueueResponse queueInvoice(@PathVariable String orderNumber) {
        return WhatsappQueueResponse.from(whatsapp.queueInvoice(orderNumber));
    }

    @PostMapping("/invoice/{orderNumber}/pdf/queue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    WhatsappQueueResponse queueInvoicePdf(@PathVariable String orderNumber) {
        return WhatsappQueueResponse.from(whatsapp.queueInvoicePdf(orderNumber));
    }

    @PostMapping("/orders/{orderNumber}/payments/{paymentNumber}/receipt")
    @ResponseStatus(HttpStatus.ACCEPTED)
    WhatsappQueueResponse sendReceipt(@PathVariable String orderNumber, @PathVariable String paymentNumber) {
        return WhatsappQueueResponse.from(whatsapp.sendPaymentUpdate(orderNumber, paymentNumber));
    }

    public record WhatsappQueueResponse(
            String orderNumber,
            String invoiceNumber,
            String recipientPhone,
            String mediaType,
            String status,
            String providerMessageId,
            String lastError,
            Instant queuedAt) {
        static WhatsappQueueResponse from(WhatsappMessage message) {
            return new WhatsappQueueResponse(
                    message.getOrder().getOrderNumber(), message.getOrder().getInvoiceNumber(),
                    message.getRecipientPhone(), message.getMediaType(),
                    message.getDeliveryStatus().name(), message.getProviderMessageId(),
                    message.getLastError(), message.getCreatedAt());
        }
    }
}
