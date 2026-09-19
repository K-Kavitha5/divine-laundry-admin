package com.divinelaundry.api;

import com.divinelaundry.domain.Payment;
import com.divinelaundry.domain.PaymentMode;
import com.divinelaundry.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentSummaryResponse record(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request, Principal principal) {
        if (idempotencyKey != null && !idempotencyKey.equals(request.clientRequestId())) {
            throw new IllegalArgumentException("Idempotency key does not match the request ID");
        }
        return PaymentSummaryResponse.from(paymentService.record(new PaymentService.RecordPaymentCommand(
                request.clientRequestId(), request.orderNumber(), request.mode(), request.transactionReference(),
                request.amount(), request.paidAt(), principal.getName())));
    }

    @GetMapping("/order/{orderNumber}")
    PaymentSummaryResponse summary(@PathVariable String orderNumber) {
        return PaymentSummaryResponse.from(paymentService.summary(orderNumber));
    }

    public record PaymentRequest(
            @NotBlank String clientRequestId,
            @NotBlank String orderNumber,
            @NotNull PaymentMode mode,
            String transactionReference,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            Instant paidAt,
            String createdBy) {}

    public record PaymentRow(
            String paymentNumber,
            String mode,
            String transactionReference,
            BigDecimal amount,
            Instant paidAt,
            String createdBy) {
        static PaymentRow from(Payment payment) {
            return new PaymentRow(
                    payment.getPaymentNumber(), payment.getMode().name(), payment.getTransactionReference(),
                    payment.getAmount(), payment.getPaidAt(), payment.getCreatedBy());
        }
    }

    public record PaymentSummaryResponse(
            String orderNumber,
            String invoiceNumber,
            String customerName,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal balance,
            String paymentStatus,
            List<PaymentRow> payments) {
        static PaymentSummaryResponse from(PaymentService.PaymentSummary summary) {
            return new PaymentSummaryResponse(
                    summary.order().getOrderNumber(), summary.order().getInvoiceNumber(),
                    summary.order().getCustomer().getName(), summary.order().getTotal(),
                    summary.amountPaid(), summary.balance(), summary.order().getPaymentStatus().name(),
                    summary.payments().stream().map(PaymentRow::from).toList());
        }
    }
}
