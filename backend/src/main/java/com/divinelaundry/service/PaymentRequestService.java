package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class PaymentRequestService {
    final PaymentRequestRepository requests;
    final LaundryOrderRepository orders;
    final PaymentService paymentService;
    final PaymentProvider provider;
    final TransactionTemplate transactionTemplate;

    public PaymentRequestService(
            PaymentRequestRepository requests,
            LaundryOrderRepository orders,
            PaymentService paymentService,
            PaymentProvider provider) {
        this(requests, orders, paymentService, provider, null);
    }

    @Autowired
    public PaymentRequestService(
            PaymentRequestRepository requests,
            LaundryOrderRepository orders,
            PaymentService paymentService,
            PaymentProvider provider,
            PlatformTransactionManager transactionManager) {
        this.requests = requests;
        this.orders = orders;
        this.paymentService = paymentService;
        this.provider = provider;
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
    }

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

        PaymentRequest request = inTransaction(() -> {
            Optional<PaymentRequest> duplicateOnSave = requests.findByIdempotencyKey(idempotencyKey);
            if (duplicateOnSave.isPresent()) {
                return duplicateOnSave.get();
            }

            List<PaymentRequest> active = requests.findByOrderIdAndStatusInOrderByCreatedAtDesc(
                    order.getId(), List.of(PaymentRequestStatus.CREATED, PaymentRequestStatus.PENDING));
            if (!active.isEmpty()) {
                throw new IllegalStateException("A payment request already exists for this order");
            }

            PaymentRequest created = new PaymentRequest(order, amount, "INR", "mock-local", actor, idempotencyKey);
            return requests.save(created);
        });

        PaymentProvider.ProviderPaymentResponse providerResponse = provider.createPaymentRequest(
                new PaymentProvider.PaymentRequestContext(
                        order.getOrderNumber(),
                        order.getInvoiceNumber(),
                        order.getCustomer().getName(),
                        amount,
                        "INR",
                        actor,
                        idempotencyKey));

        if (providerResponse == null) {
            throw new IllegalStateException("Payment provider returned no response");
        }
        if (providerResponse.providerReference() == null || providerResponse.providerReference().isBlank()) {
            throw new IllegalStateException("Payment provider did not return a request reference");
        }
        if (providerResponse.requestedAmount() != null && providerResponse.requestedAmount().compareTo(amount) != 0) {
            throw new IllegalStateException("Provider amount mismatch for payment request");
        }
        if (providerResponse.currency() == null || !providerResponse.currency().equalsIgnoreCase("INR")) {
            throw new IllegalStateException("Provider currency mismatch for payment request");
        }
        if (providerResponse.provider() == null || providerResponse.provider().isBlank()) {
            throw new IllegalStateException("Payment provider did not identify itself");
        }

        return inTransaction(() -> {
            PaymentRequest persisted = requests.findById(request.getId())
                    .orElse(request);
            persisted.markPending(providerResponse.providerReference(), providerResponse.expiresAt());
            persisted.setProvider(providerResponse.provider());
            PaymentRequest saved = requests.save(persisted);
            return saved == null ? persisted : saved;
        });
    }

    public PaymentRequest confirmVerifiedPayment(String orderNumber, String providerReference, BigDecimal amount, String currency, String actor) {
        validateAmount(amount);
        PaymentRequest request = requests.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found for provider reference " + providerReference));

        if (request.getStatus() == PaymentRequestStatus.PAID) {
            return request;
        }
        if (!request.getOrderNumber().equals(orderNumber)) {
            throw new IllegalStateException("Payment request does not belong to the provided order");
        }

        PaymentProvider.ProviderPaymentResponse verification = provider.verifyPayment(providerReference, amount, currency);
        if (verification == null) {
            throw new IllegalStateException("Payment verification returned no response");
        }
        if (verification.providerReference() != null && !verification.providerReference().equals(providerReference)) {
            throw new IllegalStateException("Payment request identity mismatch for provider response");
        }
        if (verification.status() != PaymentRequestStatus.PAID) {
            String reason = verification.failureReason() == null || verification.failureReason().isBlank()
                    ? "Payment verification failed"
                    : verification.failureReason();
            request.markFailed(reason);
            inTransaction(() -> requests.save(request));
            throw new IllegalStateException(reason);
        }

        if (request.getRequestedAmount().compareTo(amount) != 0) {
            throw new IllegalStateException("Amount mismatch for payment request");
        }
        if (!request.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalStateException("Currency mismatch for payment request");
        }
        if (verification.requestedAmount() != null && verification.requestedAmount().compareTo(request.getRequestedAmount()) != 0) {
            throw new IllegalStateException("Provider amount mismatch for payment request");
        }
        if (verification.currency() == null || !verification.currency().equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalStateException("Provider currency mismatch for payment request");
        }
        if (verification.providerPaymentId() != null) {
            Optional<PaymentRequest> duplicateProviderPayment = requests.findByProviderPaymentId(verification.providerPaymentId());
            if (duplicateProviderPayment.isPresent() && !duplicateProviderPayment.get().getId().equals(request.getId())) {
                throw new IllegalStateException("A payment with this provider payment ID already exists");
            }
        }

        paymentService.record(new PaymentService.RecordPaymentCommand(
                request.getIdempotencyKey(),
                orderNumber,
                PaymentMode.UPI,
                verification.providerPaymentId(),
                amount,
                Instant.now(),
                actor));

        PaymentRequest updated = inTransaction(() -> {
            PaymentRequest persisted = requests.findById(request.getId()).orElse(request);
            persisted.markPaid(verification.providerPaymentId());
            PaymentRequest saved = requests.save(persisted);
            return saved == null ? persisted : saved;
        });
        return updated;
    }

    public List<PaymentRequest> findByOrderNumber(String orderNumber) {
        LaundryOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return requests.findByOrderIdOrderByCreatedAtDesc(order.getId());
    }

    private <T> T inTransaction(Supplier<T> action) {
        if (transactionTemplate == null) {
            return action.get();
        }
        return transactionTemplate.execute(status -> action.get());
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
