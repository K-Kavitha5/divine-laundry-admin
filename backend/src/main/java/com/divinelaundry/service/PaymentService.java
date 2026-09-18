package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final LaundryOrderRepository orders;
    private final ApplicationEventPublisher events;

    public PaymentService(
            PaymentRepository payments,
            LaundryOrderRepository orders,
            ApplicationEventPublisher events) {
        this.payments = payments;
        this.orders = orders;
        this.events = events;
    }

    @Transactional
    public PaymentSummary record(RecordPaymentCommand command) {
        Payment existing = payments.findByClientRequestId(command.clientRequestId()).orElse(null);
        if (existing != null) {
            if (!existing.getOrder().getOrderNumber().equals(command.orderNumber())) {
                throw new IllegalArgumentException("Payment request belongs to another order");
            }
            return summary(existing.getOrder());
        }
        if (command.amount() == null || command.amount().signum() <= 0 || command.amount().scale() > 2) {
            throw new IllegalArgumentException("Payment must be positive with at most two decimal places");
        }

        LaundryOrder order = orders.findByOrderNumber(command.orderNumber())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getWorkStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled order cannot receive payment");
        }

        BigDecimal paidBefore = payments.sumByOrderId(order.getId());
        BigDecimal balance = order.getTotal().subtract(paidBefore).max(BigDecimal.ZERO);
        if (command.amount().compareTo(balance) > 0) {
            throw new IllegalArgumentException("Payment exceeds the remaining balance of " + balance);
        }

        Payment payment = new Payment(
                command.clientRequestId(), order, command.mode(), command.transactionReference(),
                command.amount(), command.paidAt(), command.createdBy());
        payment.assignPaymentNumber("PAY-%d-%s".formatted(
                currentYear(), UUID.randomUUID().toString().substring(0, 12).toUpperCase()));
        payments.save(payment);
        order.recordPayment(paidBefore.add(command.amount()));
        events.publishEvent(new PaymentRecordedEvent(order.getOrderNumber(), payment.getPaymentNumber()));
        return summary(order);
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary(String orderNumber) {
        LaundryOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return summary(order);
    }

    private PaymentSummary summary(LaundryOrder order) {
        List<Payment> rows = payments.findByOrderOrderNumberOrderByPaidAtDesc(order.getOrderNumber());
        BigDecimal paid = rows.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = order.getTotal().subtract(paid).max(BigDecimal.ZERO);
        return new PaymentSummary(order, paid, balance, rows);
    }

    private static int currentYear() {
        return Instant.now().atZone(ZoneOffset.UTC).getYear();
    }

    public record RecordPaymentCommand(
            String clientRequestId,
            String orderNumber,
            PaymentMode mode,
            String transactionReference,
            BigDecimal amount,
            Instant paidAt,
            String createdBy) {}

    public record PaymentSummary(
            LaundryOrder order,
            BigDecimal amountPaid,
            BigDecimal balance,
            List<Payment> payments) {}
}
