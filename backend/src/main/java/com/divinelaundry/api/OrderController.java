package com.divinelaundry.api;

import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.OrderItem;
import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request, Principal principal) {
        if (idempotencyKey != null && !idempotencyKey.equals(request.clientRequestId())) {
            throw new IllegalArgumentException("Idempotency key does not match the request ID");
        }
        OrderService.CreateOrderCommand command = new OrderService.CreateOrderCommand(
            request.clientRequestId(), request.customerId(), request.pickupAt(), request.deliveryAt(), request.notes(),
            principal.getName(), request.discount(), request.tax(),
                request.items().stream().map(i -> new OrderService.CreateOrderItem(
                        i.serviceId(), i.billableQuantity(), i.pieceCount(), i.noPrint())).toList());
        return OrderResponse.from(orderService.create(command));
    }

    @GetMapping
    List<OrderResponse> list(@RequestParam List<OrderStatus> status) {
        return orderService.listByStatus(status).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{orderNumber}")
    OrderResponse get(@PathVariable String orderNumber) {
        return OrderResponse.from(orderService.get(orderNumber));
    }

    @GetMapping("/customer/{customerId}/open")
    List<OrderResponse> openForCustomer(@PathVariable Long customerId) {
        return orderService.openOrdersForCustomer(customerId).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{orderNumber}/finalize")
    OrderResponse finalizeInvoice(@PathVariable String orderNumber) {
        return OrderResponse.from(orderService.finalizeInvoice(orderNumber));
    }

    @PatchMapping("/{orderNumber}/status")
    OrderResponse changeStatus(@PathVariable String orderNumber, @RequestBody StatusRequest request, Principal principal) {
        return OrderResponse.from(orderService.changeStatus(orderNumber, request.status(), principal.getName()));
    }

    public record CreateOrderRequest(
            @NotBlank String clientRequestId,
            @NotNull Long customerId,
            Instant pickupAt,
            Instant deliveryAt,
            String notes,
            String createdBy,
            @PositiveOrZero BigDecimal discount,
            @PositiveOrZero BigDecimal tax,
            @NotEmpty List<@Valid CreateOrderItemRequest> items) {}

    public record CreateOrderItemRequest(
            @NotNull Long serviceId,
            @NotNull @DecimalMin("0.001") BigDecimal billableQuantity,
            @PositiveOrZero int pieceCount,
            boolean noPrint) {}

    public record StatusRequest(@NotNull OrderStatus status) {}

    public record OrderItemResponse(
            String serviceCode, String serviceName, String unit, BigDecimal rate,
            BigDecimal quantity, int pieces, BigDecimal lineTotal, boolean noPrint) {
        static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getServiceCode(), i.getServiceName(), i.getPricingUnit().name(),
                    i.getUnitRate(), i.getBillableQuantity(), i.getPieceCount(), i.getLineTotal(), i.isNoPrint());
        }
    }

    public record OrderResponse(
            String orderNumber, String invoiceNumber, Long customerId, String customerName,
            String status, String paymentStatus, Instant placedAt, Instant pickupAt, Instant deliveryAt,
            BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal roundOff,
            BigDecimal total, List<OrderItemResponse> items) {
        static OrderResponse from(LaundryOrder o) {
            return new OrderResponse(o.getOrderNumber(), o.getInvoiceNumber(), o.getCustomer().getId(),
                    o.getCustomer().getName(), o.getWorkStatus().name(), o.getPaymentStatus().name(),
                    o.getPlacedAt(), o.getPickupAt(), o.getDeliveryAt(), o.getSubtotal(), o.getDiscount(), o.getTax(),
                    o.getRoundOff(), o.getTotal(), o.getItems().stream().map(OrderItemResponse::from).toList());
        }
    }
}
