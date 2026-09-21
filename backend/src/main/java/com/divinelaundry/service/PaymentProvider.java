package com.divinelaundry.service;

import com.divinelaundry.domain.PaymentRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentProvider {
    ProviderPaymentResponse createPaymentRequest(PaymentRequestContext context);

    ProviderPaymentResponse verifyPayment(String providerReference, BigDecimal amount, String currency);

    ProviderPaymentResponse cancelPaymentRequest(String providerReference);

    ProviderPaymentResponse getPaymentStatus(String providerReference);

    record PaymentRequestContext(
            String orderNumber,
            String invoiceNumber,
            String customerName,
            BigDecimal requestedAmount,
            String currency,
            String actor,
            String idempotencyKey) {}

    record ProviderPaymentResponse(
            String provider,
            String providerReference,
            String providerPaymentId,
            BigDecimal requestedAmount,
            BigDecimal paidAmount,
            String currency,
            PaymentRequestStatus status,
            String paymentUrl,
            String qrPayload,
            String failureReason,
            Instant expiresAt) {}
}
