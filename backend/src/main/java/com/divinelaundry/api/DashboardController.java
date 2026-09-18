package com.divinelaundry.api;

import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.domain.PaymentStatus;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final List<OrderStatus> IN_PROCESS = List.of(
            OrderStatus.RECEIVED, OrderStatus.WASHING, OrderStatus.IRONING,
            OrderStatus.CLEANED, OrderStatus.REWORK);
    private static final List<OrderStatus> OPEN = List.of(
            OrderStatus.DRAFT, OrderStatus.RECEIVED, OrderStatus.WASHING,
            OrderStatus.IRONING, OrderStatus.CLEANED, OrderStatus.READY, OrderStatus.REWORK);
    private static final List<OrderStatus> EXCLUDED_FROM_SALES = List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED);
    private static final List<PaymentStatus> OUTSTANDING = List.of(PaymentStatus.UNPAID, PaymentStatus.PARTIAL);

    private final LaundryOrderRepository orders;
    private final PaymentRepository payments;
    private final ZoneId businessZone;

    public DashboardController(
            LaundryOrderRepository orders,
            PaymentRepository payments,
            @Value("${app.business-zone}") String businessZone) {
        this.orders = orders;
        this.payments = payments;
        this.businessZone = ZoneId.of(businessZone);
    }

    @GetMapping("/health")
    Health health() {
        return new Health("ok");
    }

    @GetMapping("/dashboard/summary")
    DashboardSummary summary() {
        Instant now = Instant.now();
        Instant start = LocalDate.now(businessZone).atStartOfDay(businessZone).toInstant();
        Instant end = LocalDate.now(businessZone).plusDays(1).atStartOfDay(businessZone).toInstant();
        return new DashboardSummary(
                orders.countSalesOrdersBetween(start, end, EXCLUDED_FROM_SALES),
                orders.countByWorkStatusIn(IN_PROCESS),
                orders.countByWorkStatus(OrderStatus.READY),
                orders.countByDeliveryAtBeforeAndWorkStatusIn(now, OPEN),
                orders.sumUnpaidBilledAmount(OUTSTANDING, List.of(OrderStatus.CANCELLED))
                        .subtract(payments.sumForOutstandingOrders(OUTSTANDING, List.of(OrderStatus.CANCELLED)))
                        .max(BigDecimal.ZERO),
                orders.countByPaymentStatusInAndWorkStatusNotIn(OUTSTANDING, List.of(OrderStatus.CANCELLED)),
                orders.sumSalesBetween(start, end, EXCLUDED_FROM_SALES),
                orders.countByWorkStatus(OrderStatus.DELIVERED));
    }

    public record Health(String status) {}
    public record DashboardSummary(
            long todayOrders,
            long inProcess,
            long ready,
            long overdue,
            BigDecimal unpaidAmount,
            long unpaidBills,
            BigDecimal todaySales,
            long delivered) {}
}
