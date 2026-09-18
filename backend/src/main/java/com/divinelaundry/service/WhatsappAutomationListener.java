package com.divinelaundry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WhatsappAutomationListener {
    private static final Logger log = LoggerFactory.getLogger(WhatsappAutomationListener.class);

    private final WhatsappService whatsapp;

    public WhatsappAutomationListener(WhatsappService whatsapp) {
        this.whatsapp = whatsapp;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invoiceFinalised(InvoiceFinalisedEvent event) {
        try {
            whatsapp.queueInvoice(event.orderNumber());
        } catch (RuntimeException error) {
            log.error("Automatic WhatsApp invoice send failed for {}", event.orderNumber(), error);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void paymentRecorded(PaymentRecordedEvent event) {
        try {
            whatsapp.sendPaymentUpdate(event.orderNumber(), event.paymentNumber());
        } catch (RuntimeException error) {
            log.error("Automatic WhatsApp payment update failed for {}", event.orderNumber(), error);
        }
    }
}
