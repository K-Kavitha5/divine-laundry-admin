package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.Payment;
import com.divinelaundry.domain.PaymentMode;
import com.divinelaundry.domain.PaymentStatus;
import com.divinelaundry.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentReceiptServiceTest {
    @Test
    void partialPaymentReceiptDerivesPreviousAndRemainingOutstanding() {
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentReceiptService service = new PaymentReceiptService(payments,
                "Divine Laundry", "000", "Trichy", "");
        LaundryOrder order = order("SO-2026-000001", "INV-2026-000001", "1000.00");
        Payment payment = payment(order, "PAY-2026-PARTIAL", "400.00");
        when(payments.findByPaymentNumber(payment.getPaymentNumber())).thenReturn(Optional.of(payment));
        when(payments.sumByOrderId(order.getId())).thenReturn(new BigDecimal("400.00"));
        order.recordPayment(new BigDecimal("400.00"));

        PaymentReceiptService.PaymentReceiptDocument receipt = service.document(order.getOrderNumber(), payment.getPaymentNumber());

        assertThat(receipt.previousOutstanding()).isEqualByComparingTo("1000.00");
        assertThat(receipt.amountReceived()).isEqualByComparingTo("400.00");
        assertThat(receipt.remainingOutstanding()).isEqualByComparingTo("600.00");
        assertThat(receipt.paymentStatus()).isEqualTo(PaymentStatus.PARTIAL.name());
        verify(payments, never()).save(any());
    }

    @Test
    void finalPaymentReceiptDerivesPreviousOutstandingAndPaidState() {
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentReceiptService service = new PaymentReceiptService(payments,
                "Divine Laundry", "000", "Trichy", "");
        LaundryOrder order = order("SO-2026-000002", "INV-2026-000002", "1000.00");
        Payment payment = payment(order, "PAY-2026-FINAL", "400.00");
        when(payments.findByPaymentNumber(payment.getPaymentNumber())).thenReturn(Optional.of(payment));
        when(payments.sumByOrderId(order.getId())).thenReturn(new BigDecimal("1000.00"));
        order.recordPayment(new BigDecimal("1000.00"));

        PaymentReceiptService.PaymentReceiptDocument receipt = service.document(order.getOrderNumber(), payment.getPaymentNumber());

        assertThat(receipt.previousOutstanding()).isEqualByComparingTo("400.00");
        assertThat(receipt.remainingOutstanding()).isEqualByComparingTo("0.00");
        assertThat(receipt.paymentStatus()).isEqualTo(PaymentStatus.PAID.name());
    }

    @Test
    void paymentFromAnotherOrderIsNotExposed() {
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentReceiptService service = new PaymentReceiptService(payments,
                "Divine Laundry", "000", "Trichy", "");
        LaundryOrder order = order("SO-2026-000003", "INV-2026-000003", "1000.00");
        Payment payment = payment(order, "PAY-2026-CROSS", "100.00");
        when(payments.findByPaymentNumber(payment.getPaymentNumber())).thenReturn(Optional.of(payment));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.document("SO-2026-OTHER", payment.getPaymentNumber()))
                .isInstanceOf(PaymentReceiptService.PaymentReceiptNotFoundException.class);
    }

    private static LaundryOrder order(String number, String invoice, String total) {
        LaundryOrder order = new LaundryOrder("receipt-" + number, new Customer("Receipt Customer", "9876543210", "Street", "Trichy"), null, null, "admin");
        order.assignOrderNumber(number);
        order.finalizeInvoice(invoice);
        order.recordPayment(BigDecimal.ZERO);
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        setTotal(order, new BigDecimal(total));
        return order;
    }

    private static Payment payment(LaundryOrder order, String number, String amount) {
        Payment payment = new Payment("request-" + number, order, PaymentMode.CASH, "REF-1",
                new BigDecimal(amount), Instant.parse("2026-09-20T10:00:00Z"), "admin");
        payment.assignPaymentNumber(number);
        return payment;
    }

    private static void setTotal(LaundryOrder order, BigDecimal total) {
        // A one-line persisted item keeps the test on the same domain calculation path.
        var item = new com.divinelaundry.domain.LaundryServiceItem("RECEIPT", "Receipt service", "Test",
                com.divinelaundry.domain.PricingUnit.PIECE, total);
        order.addItem(new com.divinelaundry.domain.OrderItem(item, BigDecimal.ONE, 1, false));
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}