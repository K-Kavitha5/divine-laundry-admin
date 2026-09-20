package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.PaymentMode;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhatsappServiceTest {
    @Test
    void paymentEventPathSendsTheExactReceiptAsDocument() {
        WhatsappMessageRepository messages = mock(WhatsappMessageRepository.class);
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        DocumentService documents = mock(DocumentService.class);
        InvoicePaymentImageService images = mock(InvoicePaymentImageService.class);
        PdfInvoiceService invoices = mock(PdfInvoiceService.class);
        PaymentReceiptService receipts = mock(PaymentReceiptService.class);
        PdfReceiptService receiptPdfs = mock(PdfReceiptService.class);
        WhatsappCloudApiClient provider = mock(WhatsappCloudApiClient.class);
        WhatsappProviderProperties properties = new WhatsappProviderProperties(
                true, "https://graph.example", "v-test", "phone-id", "secret-token",
                "image-template", "en", "document-template", "en");
        WhatsappService service = new WhatsappService(messages, orders, documents, images, invoices,
                receipts, receiptPdfs, provider, properties);

        LaundryOrder order = new LaundryOrder("receipt-send", new Customer("Customer", "9876543210", null, "Trichy"), null, null, "admin");
        order.assignOrderNumber("SO-2026-000001");
        order.finalizeInvoice("INV-2026-000001");
        var receipt = new PaymentReceiptService.PaymentReceiptDocument(
                new PaymentReceiptService.BusinessDetails("Divine Laundry", "000", "Trichy", ""),
                "PAY-2026-000001", "PAY-2026-000001", Instant.parse("2026-09-20T10:00:00Z"),
                PaymentMode.CASH.name(), "REF-1", "admin", order.getOrderNumber(), order.getInvoiceNumber(),
                "Customer", "9876543210", null, "Trichy", new BigDecimal("1000.00"),
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("600.00"), "PARTIAL");
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F'};
        when(orders.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
        when(receipts.document(order.getOrderNumber(), receipt.paymentNumber())).thenReturn(receipt);
        when(receiptPdfs.render(receipt)).thenReturn(pdf);
        when(provider.isDocumentConfigured()).thenReturn(true);
        when(provider.sendInvoiceAndPaymentDocument(anyString(), eq(pdf), eq("PAY-2026-000001.pdf"), any()))
                .thenReturn(new WhatsappCloudApiClient.DeliveryResult("media-1", "wamid.1"));
        when(messages.findByDeduplicationKey("RECEIPT_PDF:PAY-2026-000001")).thenReturn(Optional.empty());
        when(messages.save(any(WhatsappMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.saveAndFlush(any(WhatsappMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WhatsappMessage result = service.sendPaymentUpdate(order.getOrderNumber(), receipt.paymentNumber());

        assertThat(result.getDeduplicationKey()).isEqualTo("RECEIPT_PDF:PAY-2026-000001");
        assertThat(result.getMediaType()).isEqualTo("DOCUMENT");
        assertThat(result.getProviderMessageId()).isEqualTo("wamid.1");
        verify(provider).sendInvoiceAndPaymentDocument(eq("9876543210"), eq(pdf),
                eq("PAY-2026-000001.pdf"), argThat(values -> values.invoiceNumber().equals("PAY-2026-000001")
                        && values.amountPaid().compareTo(new BigDecimal("400.00")) == 0));
        verifyNoInteractions(images, invoices, documents);
    }
}
