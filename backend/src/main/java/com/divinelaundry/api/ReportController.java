package com.divinelaundry.api;

import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.ServiceSalesRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final List<OrderStatus> EXCLUDED = List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED);
    private final LaundryOrderRepository orders;
    private final ZoneId businessZone;

    public ReportController(
            LaundryOrderRepository orders,
            @Value("${app.business-zone:Asia/Kolkata}") String businessZone) {
        this.orders = orders;
        this.businessZone = ZoneId.of(businessZone);
    }

    @GetMapping("/sales")
    SalesReport sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("Report end date must be on or after start date");
        Instant startAt = from.atStartOfDay(businessZone).toInstant();
        Instant endAt = to.plusDays(1).atStartOfDay(businessZone).toInstant();
        BigDecimal grossSales = orders.sumSalesBetween(startAt, endAt, EXCLUDED);
        long orderCount = orders.countSalesOrdersBetween(startAt, endAt, EXCLUDED);
        BigDecimal averageBill = orderCount == 0
                ? BigDecimal.ZERO
                : grossSales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        List<ServiceSalesRow> serviceSales = orders.serviceSalesBetween(startAt, endAt, EXCLUDED);
        return new SalesReport(from, to, grossSales, orderCount, averageBill, serviceSales);
    }

    public record SalesReport(
            LocalDate from,
            LocalDate to,
            BigDecimal grossSales,
            long orderCount,
            BigDecimal averageBill,
            List<ServiceSalesRow> serviceSales) {}
}
