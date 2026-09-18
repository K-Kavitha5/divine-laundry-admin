package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.LaundryServiceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OrderService {
    private static final List<OrderStatus> OPEN_STATUSES = List.of(
            OrderStatus.DRAFT, OrderStatus.RECEIVED, OrderStatus.WASHING,
            OrderStatus.IRONING, OrderStatus.CLEANED, OrderStatus.READY, OrderStatus.REWORK);

    private final LaundryOrderRepository orders;
    private final CustomerRepository customers;
    private final LaundryServiceRepository services;
    private final ApplicationEventPublisher events;

    public OrderService(
            LaundryOrderRepository orders,
            CustomerRepository customers,
            LaundryServiceRepository services,
            ApplicationEventPublisher events) {
        this.orders = orders;
        this.customers = customers;
        this.services = services;
        this.events = events;
    }

    @Transactional
    public LaundryOrder create(CreateOrderCommand command) {
        return orders.findByClientRequestId(command.clientRequestId()).orElseGet(() -> createNew(command));
    }

    private LaundryOrder createNew(CreateOrderCommand command) {
        Customer customer = customers.findById(command.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("At least one service item is required");
        }

        LaundryOrder order = new LaundryOrder(
                command.clientRequestId(), customer, command.deliveryAt(), command.notes(), command.createdBy());

        for (CreateOrderItem item : command.items()) {
            LaundryServiceItem service = services.findById(item.serviceId())
                    .filter(LaundryServiceItem::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Active service not found: " + item.serviceId()));
            order.addItem(new OrderItem(service, item.billableQuantity(), item.pieceCount(), item.noPrint()));
        }

        order.calculateTotals(nullToZero(command.discount()), nullToZero(command.tax()));
        LaundryOrder saved = orders.save(order);
        saved.assignOrderNumber("SO-%d-%06d".formatted(currentYear(), saved.getId()));
        return saved;
    }

    @Transactional
    public LaundryOrder finalizeInvoice(String orderNumber) {
        LaundryOrder order = get(orderNumber);
        boolean newlyFinalised = order.getInvoiceNumber() == null;
        order.finalizeInvoice("INV-%d-%06d".formatted(currentYear(), order.getId()));
        if (newlyFinalised) events.publishEvent(new InvoiceFinalisedEvent(order.getOrderNumber()));
        return order;
    }

    @Transactional
    public LaundryOrder changeStatus(String orderNumber, OrderStatus status) {
        LaundryOrder order = get(orderNumber);
        if (order.getWorkStatus() == OrderStatus.CANCELLED || order.getWorkStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Completed or cancelled orders cannot be moved directly");
        }
        order.changeStatus(status);
        return order;
    }

    @Transactional(readOnly = true)
    public LaundryOrder get(String orderNumber) {
        return orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional(readOnly = true)
    public List<LaundryOrder> openOrdersForCustomer(Long customerId) {
        return orders.findByCustomerIdAndWorkStatusInOrderByPlacedAtDesc(customerId, OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<LaundryOrder> listByStatus(List<OrderStatus> statuses) {
        return orders.findByWorkStatusInOrderByDeliveryAtAsc(statuses);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int currentYear() {
        return Instant.now().atZone(ZoneOffset.UTC).getYear();
    }

    public record CreateOrderCommand(
            String clientRequestId,
            Long customerId,
            Instant deliveryAt,
            String notes,
            String createdBy,
            BigDecimal discount,
            BigDecimal tax,
            List<CreateOrderItem> items) {}

    public record CreateOrderItem(
            Long serviceId,
            BigDecimal billableQuantity,
            int pieceCount,
            boolean noPrint) {}
}
