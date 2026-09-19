package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.LaundryServiceRepository;
import com.divinelaundry.repository.OrderStatusHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class OrderService {
    private static final List<OrderStatus> OPEN_STATUSES = List.of(
            OrderStatus.DRAFT, OrderStatus.RECEIVED, OrderStatus.WASHING,
            OrderStatus.IRONING, OrderStatus.CLEANED, OrderStatus.READY, OrderStatus.REWORK);

    private final LaundryOrderRepository orders;
    private final CustomerRepository customers;
    private final LaundryServiceRepository services;
    private final OrderStatusHistoryRepository statusHistory;
    private final ApplicationEventPublisher events;

    public OrderService(
            LaundryOrderRepository orders,
            CustomerRepository customers,
            LaundryServiceRepository services,
            OrderStatusHistoryRepository statusHistory,
            ApplicationEventPublisher events) {
        this.orders = orders;
        this.customers = customers;
        this.services = services;
        this.statusHistory = statusHistory;
        this.events = events;
    }

    @Transactional
    public LaundryOrder create(CreateOrderCommand command) {
        LaundryOrder existing = orders.findByClientRequestId(command.clientRequestId()).orElse(null);
        if (existing != null) {
            if (!sameRequest(existing, command)) {
                throw new IllegalArgumentException("This request ID was already used for a different order");
            }
            return existing;
        }
        return createNew(command);
    }

    private LaundryOrder createNew(CreateOrderCommand command) {
        Customer customer = customers.findById(command.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        if (!customer.isActive()) {
            throw new IllegalStateException("Inactive customers cannot receive new orders");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("At least one service item is required");
        }
        if (command.createdBy() == null || command.createdBy().isBlank()) {
            throw new IllegalArgumentException("Authenticated creator is required");
        }

        LaundryOrder order = new LaundryOrder(
            command.clientRequestId(), customer, command.pickupAt(), command.deliveryAt(),
            command.notes(), command.createdBy());

        for (CreateOrderItem item : command.items()) {
            LaundryServiceItem service = validateItem(item);
            order.addItem(new OrderItem(service, item.billableQuantity(), item.pieceCount(), item.noPrint()));
        }

        BigDecimal discount = nullToZero(command.discount());
        BigDecimal tax = nullToZero(command.tax());
        if (discount.signum() < 0 || tax.signum() < 0) {
            throw new IllegalArgumentException("Discount and tax cannot be negative");
        }
        order.calculateTotals(discount, tax);
        LaundryOrder saved = orders.save(order);
        saved.assignOrderNumber("SO-%d-%06d".formatted(currentYear(), saved.getId()));
        return saved;
    }

    private LaundryServiceItem validateItem(CreateOrderItem item) {
        if (item == null || item.serviceId() == null || item.billableQuantity() == null
                || item.billableQuantity().signum() <= 0 || item.pieceCount() <= 0) {
            throw new IllegalArgumentException("Each order item needs an active service, positive quantity, and positive pieces");
        }
        LaundryServiceItem service = services.findById(item.serviceId())
                .filter(LaundryServiceItem::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Select an active service for every order item"));
        if (service.getUnitRate() == null || service.getUnitRate().signum() <= 0) {
            throw new IllegalArgumentException("Selected service pricing is not configured");
        }
        if (service.getPricingUnit() == PricingUnit.PIECE
                && (item.billableQuantity().stripTrailingZeros().scale() > 0
                || item.billableQuantity().compareTo(BigDecimal.valueOf(item.pieceCount())) != 0)) {
            throw new IllegalArgumentException("Per-piece quantity must match physical pieces");
        }
        return service;
    }

    private boolean sameRequest(LaundryOrder existing, CreateOrderCommand command) {
        if (!Objects.equals(existing.getCreatedBy(), command.createdBy())
                || !Objects.equals(existing.getNotes(), command.notes())
                || !Objects.equals(existing.getPickupAt(), command.pickupAt())
                || !Objects.equals(existing.getDeliveryAt(), command.deliveryAt())
                || nullToZero(existing.getDiscount()).compareTo(nullToZero(command.discount())) != 0
                || nullToZero(existing.getTax()).compareTo(nullToZero(command.tax())) != 0) {
            return false;
        }
        if (existing.getCustomer().getId() != null && !Objects.equals(existing.getCustomer().getId(), command.customerId())) {
            return false;
        }
        if (command.items() == null) return false;
        List<OrderItem> existingItems = existing.getItems();
        if (existingItems.size() != command.items().size()) return false;
        for (int index = 0; index < existingItems.size(); index++) {
            CreateOrderItem requested = command.items().get(index);
            LaundryServiceItem service = services.findById(requested.serviceId()).orElse(null);
            OrderItem saved = existingItems.get(index);
            if (service == null || !Objects.equals(saved.getServiceCode(), service.getCode())
                    || saved.getBillableQuantity().compareTo(requested.billableQuantity()) != 0
                    || saved.getPieceCount() != requested.pieceCount()
                    || saved.isNoPrint() != requested.noPrint()) return false;
        }
        return true;
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
    public LaundryOrder changeStatus(String orderNumber, OrderStatus status, String changedBy) {
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalStateException("Authenticated admin actor is required to change order status");
        }
        LaundryOrder order = get(orderNumber);
        OrderStatus previousStatus = order.getWorkStatus();
        if (previousStatus == OrderStatus.CANCELLED || previousStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Completed or cancelled orders cannot be moved directly");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (previousStatus == status) {
            throw new IllegalStateException("Order is already in " + status + ".");
        }
        if (!OrderStatus.isValidTransition(previousStatus, status)) {
            throw new IllegalStateException("Invalid order status transition: " + previousStatus + " -> " + status);
        }

        order.changeStatus(status);
        OrderStatusHistory history = new OrderStatusHistory(order, previousStatus, status, changedBy, null);
        statusHistory.save(history);
        return order;
    }

    @Transactional
    public LaundryOrder changeStatus(String orderNumber, OrderStatus status) {
        return changeStatus(orderNumber, status, "SYSTEM");
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
            Instant pickupAt,
            Instant deliveryAt,
            String notes,
            String createdBy,
            BigDecimal discount,
            BigDecimal tax,
            List<CreateOrderItem> items) {
        public CreateOrderCommand(String clientRequestId, Long customerId, Instant deliveryAt,
                String notes, String createdBy, BigDecimal discount, BigDecimal tax,
                List<CreateOrderItem> items) {
            this(clientRequestId, customerId, null, deliveryAt, notes, createdBy, discount, tax, items);
        }
    }

    public record CreateOrderItem(
            Long serviceId,
            BigDecimal billableQuantity,
            int pieceCount,
            boolean noPrint) {}
}
