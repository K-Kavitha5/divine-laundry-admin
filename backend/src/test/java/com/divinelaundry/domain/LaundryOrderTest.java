package com.divinelaundry.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void pickupAndDeliveryTimesAreMappedTogether() {
        Instant pickup = Instant.parse("2027-01-15T10:00:00Z");
        Instant delivery = Instant.parse("2027-01-15T18:00:00Z");
        LaundryOrder order = new LaundryOrder("request-schedule", new Customer("Test Customer", "9876543210", null, "Trichy"),
                pickup, delivery, null, "admin");

        assertThat(order.getPickupAt()).isEqualTo(pickup);
        assertThat(order.getDeliveryAt()).isEqualTo(delivery);
    }

    @Test
    void pickupAndDeliveryCanEachBeOptional() {
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        Instant pickup = Instant.parse("2027-01-15T10:00:00Z");
        Instant delivery = Instant.parse("2027-01-15T18:00:00Z");

        LaundryOrder pickupOnly = new LaundryOrder("request-pickup-only", customer, pickup, null, null, "admin");
        LaundryOrder deliveryOnly = new LaundryOrder("request-delivery-only", customer, null, delivery, null, "admin");

        assertThat(pickupOnly.getPickupAt()).isEqualTo(pickup);
        assertThat(pickupOnly.getDeliveryAt()).isNull();
        assertThat(deliveryOnly.getPickupAt()).isNull();
        assertThat(deliveryOnly.getDeliveryAt()).isEqualTo(delivery);
    }

    @Test
    void pickupAfterDeliveryIsRejected() {
        assertThatThrownBy(() -> new LaundryOrder("request-invalid-schedule",
                new Customer("Test Customer", "9876543210", null, "Trichy"),
                Instant.parse("2027-01-15T19:00:00Z"), Instant.parse("2027-01-15T18:00:00Z"), null, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pickup date and time cannot be after delivery date and time");
    }

    @Test
    void deliveryBeforePickupIsRejected() {
        assertThatThrownBy(() -> new LaundryOrder("request-invalid-reverse-schedule",
                new Customer("Test Customer", "9876543210", null, "Trichy"),
                Instant.parse("2027-01-15T19:00:00Z"), Instant.parse("2027-01-15T18:00:00Z"), null, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pickup date and time cannot be after delivery date and time");
    }
}
