package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.LaundryServiceItem;
import com.divinelaundry.domain.PaymentMode;
import com.divinelaundry.domain.WhatsappDeliveryStatus;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.LaundryServiceRepository;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:whatsapp-tx;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.admin.username=admin",
        "app.admin.password=TestPassword123!",
        "app.whatsapp.enabled=true",
        "app.whatsapp.graph-base-url=https://graph.facebook.com",
        "app.whatsapp.graph-api-version=v20.0",
        "app.whatsapp.phone-number-id=123456789",
        "app.whatsapp.access-token=test-token",
        "app.whatsapp.template-name=invoice_template",
        "app.whatsapp.template-language=en",
        "app.whatsapp.document-template-name=document_template",
        "app.whatsapp.document-template-language=en",
        "app.whatsapp.pending-timeout=PT5M"
})
@ActiveProfiles("test")
class WhatsappTransactionBoundaryTest {
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private CustomerRepository customers;
    @Autowired private LaundryOrderRepository orders;
    @Autowired private LaundryServiceRepository services;
    @Autowired private WhatsappMessageRepository messages;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private WhatsappCloudApiClient provider;

    @BeforeEach
    void setUp() {
        when(provider.isConfigured()).thenReturn(true);
        when(provider.isDocumentConfigured()).thenReturn(true);
        when(provider.sendInvoiceAndPaymentImage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return new WhatsappCloudApiClient.DeliveryResult("media-image", "wamid-image");
                });
        when(provider.sendInvoiceAndPaymentDocument(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return new WhatsappCloudApiClient.DeliveryResult("media-document", "wamid-document");
                });
    }

    @Test
    void invoiceAfterCommitCreatesWhatsAppStateWithoutNoActiveTransaction() {
        LaundryOrder order = createOrder("invoice-event");

        order = orderService.finalizeInvoice(order.getOrderNumber());
        String invoiceNumber = order.getInvoiceNumber();

        Optional<WhatsappMessage> created = messages.findByDeduplicationKey("INVOICE_IMAGE:" + invoiceNumber);
        assertThat(created).isPresent();
        assertThat(created.get().getDeliveryStatus()).isEqualTo(WhatsappDeliveryStatus.SENT);
        verify(provider, atLeastOnce()).sendInvoiceAndPaymentImage(anyString(), any(), anyString(), any());
    }

    @Test
    void paymentAfterCommitCreatesWhatsAppStateWithoutNoActiveTransaction() {
        LaundryOrder order = createOrder("payment-event");
        orderService.finalizeInvoice(order.getOrderNumber());

        var paymentSummary = paymentService.record(new PaymentService.RecordPaymentCommand(
                UUID.randomUUID().toString(),
                order.getOrderNumber(),
                PaymentMode.CASH,
                "REF-100",
                new BigDecimal("10.00"),
                Instant.now(),
                "admin"));
        String paymentNumber = paymentSummary.payments().get(0).getPaymentNumber();

        Optional<WhatsappMessage> created = messages.findByDeduplicationKey("RECEIPT_PDF:" + paymentNumber);
        assertThat(created).isPresent();
        assertThat(created.get().getDeliveryStatus()).isEqualTo(WhatsappDeliveryStatus.SENT);
        verify(provider, atLeastOnce()).sendInvoiceAndPaymentDocument(anyString(), any(), anyString(), any());
    }

    @Test
    void disabledModeStillMakesNoProviderCall() {
        Mockito.when(provider.isConfigured()).thenReturn(false);
        LaundryOrder order = createOrder("disabled-mode");

        order = orderService.finalizeInvoice(order.getOrderNumber());

        Optional<WhatsappMessage> created = messages.findByDeduplicationKey("INVOICE_IMAGE:" + order.getInvoiceNumber());
        assertThat(created).isPresent();
        assertThat(created.get().getDeliveryStatus()).isEqualTo(WhatsappDeliveryStatus.WAITING_FOR_PROVIDER);
        verify(provider, never()).sendInvoiceAndPaymentImage(anyString(), any(), anyString(), any());
    }

    private LaundryOrder createOrder(String requestId) {
        String phone = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1_000_000_000L));
        Customer customer = customers.save(new Customer("Tx Customer", phone, "Test street", "Trichy"));
        LaundryServiceItem service = services.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(item -> "SHIRT_IRON".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        return orderService.create(new OrderService.CreateOrderCommand(
                requestId,
                customer.getId(),
                Instant.parse("2027-01-15T18:00:00Z"),
                "Integration test order",
                "admin",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new OrderService.CreateOrderItem(service.getId(), new BigDecimal("2"), 2, false))));
    }
}
