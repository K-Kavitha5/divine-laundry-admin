package com.divinelaundry.service;

import com.divinelaundry.domain.PaymentRequestStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Primary
public class MockPaymentProvider implements PaymentProvider {
    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, ProviderPaymentResponse> responses = new ConcurrentHashMap<>();

    @Override
    public ProviderPaymentResponse createPaymentRequest(PaymentRequestContext context) {
        String providerReference = "MOCK-REF-" + sequence.getAndIncrement();
        ProviderPaymentResponse response = new ProviderPaymentResponse(
                "mock-local",
                providerReference,
                null,
                context.requestedAmount(),
                BigDecimal.ZERO,
                context.currency(),
                PaymentRequestStatus.CREATED,
                null,
                "mock-local://payment/" + providerReference,
                null,
                Instant.now().plus(Duration.ofMinutes(15)));
        responses.put(providerReference, response);
        return response;
    }

    @Override
    public ProviderPaymentResponse verifyPayment(String providerReference, BigDecimal amount, String currency) {
        ProviderPaymentResponse known = responses.get(providerReference);
        if (known == null) {
            return new ProviderPaymentResponse(
                    "mock-local",
                    providerReference,
                    "MOCK-PAY-" + sequence.getAndIncrement(),
                    amount,
                    amount,
                    currency,
                    PaymentRequestStatus.PAID,
                    null,
                    null,
                    null,
                    Instant.now());
        }
        if (amount.compareTo(known.requestedAmount()) != 0) {
            return new ProviderPaymentResponse(
                    known.provider(),
                    known.providerReference(),
                    "MOCK-PAY-" + sequence.getAndIncrement(),
                    known.requestedAmount(),
                    BigDecimal.ZERO,
                    known.currency(),
                    PaymentRequestStatus.FAILED,
                    null,
                    null,
                    "Amount mismatch",
                    Instant.now());
        }
        if (!known.currency().equalsIgnoreCase(currency)) {
            return new ProviderPaymentResponse(
                    known.provider(),
                    known.providerReference(),
                    "MOCK-PAY-" + sequence.getAndIncrement(),
                    known.requestedAmount(),
                    BigDecimal.ZERO,
                    known.currency(),
                    PaymentRequestStatus.FAILED,
                    null,
                    null,
                    "Currency mismatch",
                    Instant.now());
        }
        ProviderPaymentResponse verified = new ProviderPaymentResponse(
                known.provider(),
                known.providerReference(),
                "MOCK-PAY-" + sequence.getAndIncrement(),
                known.requestedAmount(),
                amount,
                known.currency(),
                PaymentRequestStatus.PAID,
                null,
                null,
                null,
                Instant.now());
        responses.put(providerReference, verified);
        return verified;
    }

    @Override
    public ProviderPaymentResponse cancelPaymentRequest(String providerReference) {
        ProviderPaymentResponse existing = responses.get(providerReference);
        if (existing == null) {
            return new ProviderPaymentResponse("mock-local", providerReference, null, BigDecimal.ZERO, BigDecimal.ZERO, "INR", PaymentRequestStatus.CANCELLED, null, null, "Not found", Instant.now());
        }
        ProviderPaymentResponse cancelled = new ProviderPaymentResponse(
                existing.provider(),
                existing.providerReference(),
                existing.providerPaymentId(),
                existing.requestedAmount(),
                existing.paidAmount(),
                existing.currency(),
                PaymentRequestStatus.CANCELLED,
                null,
                null,
                null,
                Instant.now());
        responses.put(providerReference, cancelled);
        return cancelled;
    }

    @Override
    public ProviderPaymentResponse getPaymentStatus(String providerReference) {
        return responses.getOrDefault(providerReference,
                new ProviderPaymentResponse("mock-local", providerReference, null, BigDecimal.ZERO, BigDecimal.ZERO, "INR", PaymentRequestStatus.CREATED, null, null, null, Instant.now()));
    }
}
