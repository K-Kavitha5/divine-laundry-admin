package com.divinelaundry.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class LaundryOrderTest {
    @Test
    void finalizingAgainKeepsTheOriginalInvoiceNumber() {
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryOrder order = new LaundryOrder("request-1", customer, null, null, "admin");

        order.finalizeInvoice("INV-2026-000001");
        order.finalizeInvoice("INV-2026-999999");

        assertThat(order.getInvoiceNumber()).isEqualTo("INV-2026-000001");
    }

    @Test
    void totalIsRoundedAndRoundOffRemainsVisible() {
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryServiceItem service = new LaundryServiceItem(
                "WASH_IRON_KG", "Wash & Iron", "Laundry by KG", PricingUnit.KG, new BigDecimal("115.00"));
        LaundryOrder order = new LaundryOrder("request-2", customer, null, null, "admin");
        order.addItem(new OrderItem(service, new BigDecimal("1.39"), 7, false));

        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(order.getSubtotal()).isEqualByComparingTo("159.85");
        assertThat(order.getRoundOff()).isEqualByComparingTo("0.15");
        assertThat(order.getTotal()).isEqualByComparingTo("160.00");
    }

    @Test
    void paymentStatusMovesFromUnpaidToPartialAndPaid() {
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryServiceItem service = new LaundryServiceItem(
                "SHIRT_IRON", "Shirt", "Ironing", PricingUnit.PIECE, new BigDecimal("20.00"));
        LaundryOrder order = new LaundryOrder("request-payment", customer, null, null, "admin");
        order.addItem(new OrderItem(service, new BigDecimal("5"), 5, false));
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);

        order.recordPayment(new BigDecimal("40.00"));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL);

        order.recordPayment(new BigDecimal("100.00"));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
