package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePaymentImageServiceTest {
    @Test
    void rendersInvoicePaymentPngWithAmountLockedUpiUri() throws Exception {
        Customer customer = new Customer("Test Customer", "9876543210", "1 Test Street", "Trichy");
        LaundryServiceItem serviceItem = new LaundryServiceItem(
                "WASH_IRON_KG", "Wash & Iron", "Laundry by KG", PricingUnit.KG, new BigDecimal("120.00"));
        LaundryOrder order = new LaundryOrder("request-1", customer, Instant.now(), null, "admin");
        order.addItem(new OrderItem(serviceItem, new BigDecimal("2.00"), 6, false));
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        order.assignOrderNumber("SO-2026-000001");
        order.finalizeInvoice("INV-2026-000001");
        PaymentService.PaymentSummary paymentSummary = new PaymentService.PaymentSummary(
                order, BigDecimal.ZERO, new BigDecimal("240.00"), List.of());
        DocumentService.DocumentBundle bundle = new DocumentService.DocumentBundle(
                new DocumentService.BusinessDetails("Divine Laundry Trichy", "0431-000000", "Trichy", ""),
                order, paymentSummary, List.of());
        InvoicePaymentImageService renderer = new InvoicePaymentImageService(
                "Asia/Kolkata", "divinelaundry@upi", "Divine Laundry Trichy");

        byte[] png = renderer.render(bundle);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        String upi = renderer.upiPaymentUri(bundle, new BigDecimal("240.00"));

        assertThat(png).hasSizeGreaterThan(10_000);
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isGreaterThan(1000);
        assertThat(upi).startsWith("upi://pay?");
        assertThat(upi).contains("pa=divinelaundry%40upi", "am=240.00", "tr=SO-2026-000001");
    }
}
