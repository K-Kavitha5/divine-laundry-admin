package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    @Test
    void repeatedPaymentRequestReturnsExistingSummaryWithoutSavingAgain() {
        PaymentRepository payments = mock(PaymentRepository.class);
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        PaymentService service = new PaymentService(payments, orders, events);
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryOrder order = new LaundryOrder("order-request", customer, null, null, "admin");
        order.assignOrderNumber("SO-2026-000001");
        Payment existing = new Payment(
                "same-payment-request", order, PaymentMode.UPI, "UPI-1",
                new BigDecimal("50.00"), Instant.now(), "admin");
        existing.assignPaymentNumber("PAY-2026-EXISTING");
        when(payments.findByClientRequestId("same-payment-request")).thenReturn(Optional.of(existing));
        when(payments.findByOrderOrderNumberOrderByPaidAtDesc(order.getOrderNumber())).thenReturn(List.of(existing));

        PaymentService.PaymentSummary result = service.record(new PaymentService.RecordPaymentCommand(
                "same-payment-request", "SO-2026-000001", PaymentMode.UPI, "UPI-1",
                new BigDecimal("50.00"), Instant.now(), "admin"));

        assertThat(result.payments()).containsExactly(existing);
        verify(payments, never()).save(any());
        verifyNoInteractions(orders);
        verifyNoInteractions(events);
    }

    @Test
    void newPaymentPublishesOneAutomaticWhatsappUpdateEvent() {
        PaymentRepository payments = mock(PaymentRepository.class);
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        PaymentService service = new PaymentService(payments, orders, events);
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryServiceItem catalogItem = new LaundryServiceItem(
                "WASH_IRON_KG", "Wash & Iron", "Laundry by KG", PricingUnit.KG, new BigDecimal("120.00"));
        LaundryOrder order = new LaundryOrder("order-request", customer, null, null, "admin");
        order.addItem(new OrderItem(catalogItem, BigDecimal.ONE, 3, false));
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        order.assignOrderNumber("SO-2026-000001");
        order.finalizeInvoice("INV-2026-000001");
        when(payments.findByClientRequestId("new-payment-request")).thenReturn(Optional.empty());
        when(orders.findByOrderNumber("SO-2026-000001")).thenReturn(Optional.of(order));
        when(payments.sumByOrderId(order.getId())).thenReturn(BigDecimal.ZERO);
        when(payments.findByOrderOrderNumberOrderByPaidAtDesc("SO-2026-000001")).thenReturn(List.of());

        service.record(new PaymentService.RecordPaymentCommand(
                "new-payment-request", "SO-2026-000001", PaymentMode.UPI, "UPI-1",
                new BigDecimal("50.00"), Instant.now(), "admin"));

        verify(payments).save(any(Payment.class));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentRecordedEvent.class);
        PaymentRecordedEvent update = (PaymentRecordedEvent) event.getValue();
        assertThat(update.orderNumber()).isEqualTo("SO-2026-000001");
        assertThat(update.paymentNumber()).startsWith("PAY-");
    }
}
