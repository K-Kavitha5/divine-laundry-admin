package com.divinelaundry.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Profile("test")
public class MockPaymentProviderTestSupport {
    private final MockPaymentProvider provider;

    public MockPaymentProviderTestSupport(MockPaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentProvider.ProviderPaymentResponse verify(String providerReference, BigDecimal amount, String currency) {
        return provider.completeForTest(providerReference, amount, currency);
    }
}
