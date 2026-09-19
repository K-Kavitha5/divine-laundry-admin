package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        @Test
        void partialAndFinalPaymentsUpdateTheOrderLifecycle() {
                PaymentRepository payments = mock(PaymentRepository.class);
                LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
                PaymentService service = new PaymentService(payments, orders, mock(ApplicationEventPublisher.class));
                LaundryOrder order = billableOrder();
                when(orders.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
                when(payments.findByClientRequestId(anyString())).thenReturn(Optional.empty());
                when(payments.sumByOrderId(order.getId())).thenReturn(BigDecimal.ZERO, new BigDecimal("50.00"));
                List<Payment> recorded = new ArrayList<>();
                when(payments.findByOrderOrderNumberOrderByPaidAtDesc(order.getOrderNumber())).thenReturn(recorded);
                when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
                        Payment payment = invocation.getArgument(0);
                        recorded.add(0, payment);
                        return payment;
                });

                PaymentService.PaymentSummary partial = service.record(command("partial-1", new BigDecimal("50.00"), "client"));
                PaymentService.PaymentSummary paid = service.record(command("partial-2", new BigDecimal("70.00"), "client"));

                assertThat(partial.balance()).isEqualByComparingTo("70.00");
                assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(paid.balance()).isEqualByComparingTo("0.00");
                assertThat(PaymentService.outstanding(order.getTotal(), new BigDecimal("130.00")))
                                .isEqualByComparingTo("0.00");
                verify(payments, times(2)).save(any(Payment.class));
        }

        @Test
        void rejectsZeroNegativeOverPreciseAndOverpayingAmounts() {
                PaymentRepository payments = mock(PaymentRepository.class);
                LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
                PaymentService service = new PaymentService(payments, orders, mock(ApplicationEventPublisher.class));
                LaundryOrder order = billableOrder();
                when(orders.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
                when(payments.findByClientRequestId(anyString())).thenReturn(Optional.empty());
                when(payments.sumByOrderId(order.getId())).thenReturn(BigDecimal.ZERO);

                assertThatThrownBy(() -> service.record(command("zero", BigDecimal.ZERO, "client")))
                                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("greater than zero");
                assertThatThrownBy(() -> service.record(command("negative", new BigDecimal("-1.00"), "client")))
                                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("greater than zero");
                assertThatThrownBy(() -> service.record(command("precise", new BigDecimal("1.001"), "client")))
                                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("two decimal places");
                assertThatThrownBy(() -> service.record(command("over", new BigDecimal("120.01"), "client")))
                                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outstanding balance");
                verify(payments, never()).save(any());
        }

        @Test
        void rejectsPaymentWhenOrderIsAlreadyFullyPaid() {
                PaymentRepository payments = mock(PaymentRepository.class);
                LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
                PaymentService service = new PaymentService(payments, orders, mock(ApplicationEventPublisher.class));
                LaundryOrder order = billableOrder();
                when(orders.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
                when(payments.findByClientRequestId(anyString())).thenReturn(Optional.empty());
                when(payments.sumByOrderId(order.getId())).thenReturn(order.getTotal());

                assertThatThrownBy(() -> service.record(command("already-paid", BigDecimal.ONE, "client")))
                                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no outstanding balance");
                verify(payments, never()).save(any());
        }

        @Test
        void paymentStoresTheServiceActorRatherThanAClientAuditValue() {
                PaymentRepository payments = mock(PaymentRepository.class);
                LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
                PaymentService service = new PaymentService(payments, orders, mock(ApplicationEventPublisher.class));
                LaundryOrder order = billableOrder();
                when(orders.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
                when(payments.findByClientRequestId(anyString())).thenReturn(Optional.empty());
                when(payments.sumByOrderId(order.getId())).thenReturn(BigDecimal.ZERO);
                when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

                service.record(command("actor", new BigDecimal("10.00"), "authenticated-admin"));

                ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
                verify(payments).save(saved.capture());
                assertThat(saved.getValue().getCreatedBy()).isEqualTo("authenticated-admin");
        }

        private static LaundryOrder billableOrder() {
                Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
                LaundryServiceItem item = new LaundryServiceItem(
                                "WASH_IRON_KG", "Wash & Iron", "Laundry by KG", PricingUnit.KG, new BigDecimal("120.00"));
                LaundryOrder order = new LaundryOrder("order-request-" + System.nanoTime(), customer, null, null, "admin");
                order.addItem(new OrderItem(item, BigDecimal.ONE, 1, false));
                order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
                order.assignOrderNumber("SO-2026-000001");
                return order;
        }

        private static PaymentService.RecordPaymentCommand command(String requestId, BigDecimal amount, String actor) {
                return new PaymentService.RecordPaymentCommand(
                                requestId, "SO-2026-000001", PaymentMode.CASH, null, amount, Instant.now(), actor);
        }
}
