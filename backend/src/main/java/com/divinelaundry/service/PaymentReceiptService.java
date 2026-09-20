package com.divinelaundry.service;

import com.divinelaundry.domain.Payment;
import com.divinelaundry.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentReceiptService {
    private final PaymentRepository payments;
    private final BusinessDetails business;

    public PaymentReceiptService(
            PaymentRepository payments,
            @Value("${app.business.name}") String name,
            @Value("${app.business.phone}") String phone,
            @Value("${app.business.address}") String address,
            @Value("${app.business.gst-number:}") String gstNumber) {
        this.payments = payments;
        this.business = new BusinessDetails(name, phone, address, gstNumber);
    }

    @Transactional(readOnly = true)
    public PaymentReceiptDocument document(String orderNumber, String paymentNumber) {
        Payment payment = payments.findByPaymentNumber(paymentNumber)
                .filter(row -> row.getOrder() != null
                        && orderNumber.equals(row.getOrder().getOrderNumber()))
                .orElseThrow(PaymentReceiptNotFoundException::new);
        BigDecimal currentPaidTotal = nonNegative(payments.sumByOrderId(payment.getOrder().getId()));
        BigDecimal remaining = currentOutstanding(payment.getOrder().getTotal(), currentPaidTotal);
        BigDecimal amountReceived = payment.getAmount();
        return new PaymentReceiptDocument(
                business,
                payment.getPaymentNumber(),
                payment.getPaymentNumber(),
                payment.getPaidAt(),
                payment.getMode().name(),
                payment.getTransactionReference(),
                payment.getCreatedBy(),
                payment.getOrder().getOrderNumber(),
                payment.getOrder().getInvoiceNumber(),
                payment.getOrder().getCustomer().getName(),
                payment.getOrder().getCustomer().getPhone(),
                payment.getOrder().getCustomer().getAddressLine(),
                payment.getOrder().getCustomer().getArea(),
                payment.getOrder().getTotal(),
                remaining.add(amountReceived),
                amountReceived,
                remaining,
                payment.getOrder().getPaymentStatus().name());
    }

    private static BigDecimal currentOutstanding(BigDecimal total, BigDecimal paid) {
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    public record BusinessDetails(String name, String phone, String address, String gstNumber) {}

    public record PaymentReceiptDocument(
            BusinessDetails business,
            String receiptNumber,
            String paymentNumber,
            java.time.Instant paymentDate,
            String paymentMode,
            String transactionReference,
            String createdBy,
            String orderNumber,
            String invoiceNumber,
            String customerName,
            String customerPhone,
            String customerAddress,
            String customerArea,
            BigDecimal orderTotal,
            BigDecimal previousOutstanding,
            BigDecimal amountReceived,
            BigDecimal remainingOutstanding,
            String paymentStatus) {}

    public static class PaymentReceiptNotFoundException extends RuntimeException {}
}