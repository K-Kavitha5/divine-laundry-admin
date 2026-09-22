package com.divinelaundry.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:default-payment-provider;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.admin.username=admin",
        "app.admin.password=TestPassword123!",
        "app.whatsapp.enabled=false",
        "app.payment.upi-id="
})
class DefaultPaymentProviderBeanTest {

    @Autowired
    private List<PaymentProvider> paymentProviders;

    @Test
    void defaultProfileExposesExactlyOneUsablePaymentProviderBean() {
        assertThat(paymentProviders)
                .hasSize(1)
                .allSatisfy(provider -> assertThat(provider).isInstanceOf(ProductionPaymentProvider.class));

        PaymentProvider provider = paymentProviders.getFirst();
        assertThatThrownBy(() -> provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        "SO-2026-000001",
                        "INV-2026-000001",
                        "Test Customer",
                        new BigDecimal("100.00"),
                        "INR",
                        "admin",
                        "idempotency-key-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Real payment provider not configured");
    }
}
