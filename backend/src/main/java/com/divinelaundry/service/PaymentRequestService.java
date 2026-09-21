package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentRequestService {
    final PaymentRequestRepository requests;
    final LaundryOrderRepository orders;
    final PaymentService paymentService;
    final PaymentProvider provider;

    public PaymentRequestService(
            PaymentRequestRepository requests,
            LaundryOrderRepository orders,
            PaymentService paymentService,
            PaymentProvider provider) {
        this.requests = requests;
        this.orders = orders;
        this.paymentService = paymentService;
        this.provider = provider;
    }

    @Transactional
    public PaymentRequest createPaymentRequest(String orderNumber, BigDecimal amount, String actor, String idempotencyKey) {
        validateAmount(amount);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        Optional<PaymentRequest> duplicateIdempotency = requests.findByIdempotencyKey(idempotencyKey);
        if (duplicateIdempotency.isPresent()) {
            return duplicateIdempotency.get();
        }

        LaundryOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getWorkStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled order cannot receive a payment request");
        }

        PaymentService.PaymentSummary summary = paymentService.summary(orderNumber);
        BigDecimal outstanding = summary.balance();
        if (outstanding.signum() == 0) {
            throw new IllegalStateException("Order has no outstanding balance");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Requested amount cannot exceed outstanding balance of " + outstanding);
        }

        List<PaymentRequest> active = requests.findByOrderIdAndStatusInOrderByCreatedAtDesc(
                order.getId(), List.of(PaymentRequestStatus.CREATED, PaymentRequestStatus.PENDING));
        if (!active.isEmpty()) {
            throw new IllegalStateException("A payment request already exists for this order");
        }

        PaymentRequest request = new PaymentRequest(order, amount, "INR", "mock-local", actor, idempotencyKey);
        requests.save(request);

        PaymentProvider.ProviderPaymentResponse providerResponse = provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        order.getOrderNumber(),
                        order.getInvoiceNumber(),
                        order.getCustomer().getName(),
                        amount,
                        "INR",
                        actor,
                        idempotencyKey));

        request.markPending(providerResponse.providerReference(), providerResponse.expiresAt());
        request.setProvider(providerResponse.provider());
        requests.save(request);
        return request;
    }

    @Transactional
    public PaymentRequest confirmVerifiedPayment(String orderNumber, String providerReference, BigDecimal amount, String currency, String actor) {
        validateAmount(amount);
        PaymentRequest request = requests.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found for provider reference " + providerReference));

        if (!request.getOrderNumber().equals(orderNumber)) {
            throw new IllegalStateException("Payment request does not belong to the provided order");
        }

        PaymentProvider.ProviderPaymentResponse verification = provider.verifyPayment(providerReference, amount, currency);
        if (verification.status() != PaymentRequestStatus.PAID) {
            String reason = verification.failureReason() == null || verification.failureReason().isBlank()
                    ? "Payment verification failed"
                    : verification.failureReason();
            request.markFailed(reason);
            requests.save(request);
            throw new IllegalStateException(reason);
        }

        if (request.getRequestedAmount().compareTo(amount) != 0) {
            throw new IllegalStateException("Amount mismatch for payment request");
        }
        if (!request.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalStateException("Currency mismatch for payment request");
        }

        paymentService.record(new PaymentService.RecordPaymentCommand(
                request.getIdempotencyKey(),
                orderNumber,
                PaymentMode.UPI,
                verification.providerPaymentId(),
                amount,
                Instant.now(),
                actor));

        request.markPaid(verification.providerPaymentId());
        requests.save(request);
        return request;
    }

    @Transactional(readOnly = true)
    public List<PaymentRequest> findByOrderNumber(String orderNumber) {
        LaundryOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return requests.findByOrderIdOrderByCreatedAtDesc(order.getId());
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Requested amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Requested amount cannot have more than two decimal places");
        }
    }
}
