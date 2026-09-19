package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.LaundryServiceItem;
import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.domain.OrderStatusHistory;
import com.divinelaundry.domain.PricingUnit;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.LaundryServiceRepository;
import com.divinelaundry.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Test
    void repeatedClientRequestReturnsTheOriginalOrderWithoutCreatingAnotherBill() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, events);
        LaundryOrder existing = new LaundryOrder(
                "same-browser-request", new Customer("Test Customer", "9876543210", null, "Trichy"),
                null, null, "admin");
        when(orders.findByClientRequestId("same-browser-request")).thenReturn(Optional.of(existing));

        OrderService.CreateOrderCommand repeated = new OrderService.CreateOrderCommand(
                "same-browser-request", 99L, null, null, "admin",
                BigDecimal.ZERO, BigDecimal.ZERO, List.of());

        LaundryOrder result = orderService.create(repeated);

        assertThat(result).isSameAs(existing);
        verify(orders, never()).save(any());
        verifyNoInteractions(customers, services);
    }

    @Test
    void newlyFinalisedInvoicePublishesAutomaticWhatsappEventOnce() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, events);
        LaundryOrder order = mock(LaundryOrder.class);
        when(order.getInvoiceNumber()).thenReturn(null);
        when(order.getId()).thenReturn(42L);
        when(order.getOrderNumber()).thenReturn("SO-2026-000042");
        when(orders.findByOrderNumber("SO-2026-000042")).thenReturn(Optional.of(order));

        orderService.finalizeInvoice("SO-2026-000042");

        verify(order).finalizeInvoice(anyString());
        verify(events).publishEvent(new InvoiceFinalisedEvent("SO-2026-000042"));
    }

        @Test
        void rejectsInvalidStatusTransitionsAndDoesNotCreateHistory() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        LaundryOrder order = new LaundryOrder("status-invalid", new Customer("Test Customer", "9876543210", null, "Trichy"), null, null, "admin");
        when(orders.findByOrderNumber("SO-2026-000001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus("SO-2026-000001", OrderStatus.DELIVERED, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RECEIVED -> DELIVERED");
        verify(history, never()).save(any());
        }

        @Test
        void validTransitionsPersistHistoryWithAuthenticatedActor() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        when(history.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        LaundryOrder order = new LaundryOrder("status-valid", new Customer("Test Customer", "9876543210", null, "Trichy"), null, null, "admin");
        when(orders.findByOrderNumber("SO-2026-000003")).thenReturn(Optional.of(order));

        LaundryOrder updated = orderService.changeStatus("SO-2026-000003", OrderStatus.WASHING, "supervisor");

        assertThat(updated.getWorkStatus()).isEqualTo(OrderStatus.WASHING);
        ArgumentCaptor<OrderStatusHistory> saved = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(history).save(saved.capture());
        assertThat(saved.getValue().getOldStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(saved.getValue().getNewStatus()).isEqualTo(OrderStatus.WASHING);
        assertThat(saved.getValue().getChangedBy()).isEqualTo("supervisor");
        }

        @Test
        void sameStatusUpdateIsRejected() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        LaundryOrder order = new LaundryOrder("status-same", new Customer("Test Customer", "9876543210", null, "Trichy"), null, null, "admin");
        when(orders.findByOrderNumber("SO-2026-000004")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus("SO-2026-000004", OrderStatus.RECEIVED, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in RECEIVED");
        verify(history, never()).save(any());
        }

        @Test
        void rejectsInvalidItemsAndMoneyBeforeSaving() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryServiceItem service = new LaundryServiceItem("SHIRT", "Shirt", "Ironing", PricingUnit.PIECE, BigDecimal.TEN);
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(services.findById(1L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> orderService.create(command(1, -1, 1, BigDecimal.ZERO, BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.create(command(1, 1, 2, BigDecimal.ZERO, BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.create(command(1, 1, 1, new BigDecimal("-1"), BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.create(command(1, 1, 1, BigDecimal.ZERO, new BigDecimal("-1"))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.create(command(1, 1, 1, new BigDecimal("11"), BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(orders, never()).save(any());
        }

        @Test
        void createsOrderWithPickupAndDeliveryTimes() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryServiceItem service = new LaundryServiceItem("SHIRT", "Shirt", "Ironing", PricingUnit.PIECE, BigDecimal.TEN);
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(services.findById(1L)).thenReturn(Optional.of(service));
        when(orders.save(any(LaundryOrder.class))).thenAnswer(invocation -> {
            LaundryOrder saved = invocation.getArgument(0);
            saved.assignOrderNumber("SO-2027-000001");
            return saved;
        });

        Instant pickup = Instant.parse("2027-01-15T10:00:00Z");
        Instant delivery = Instant.parse("2027-01-15T18:00:00Z");
        LaundryOrder saved = orderService.create(new OrderService.CreateOrderCommand(
            "schedule-request", 1L, pickup, delivery, null, "admin", BigDecimal.ZERO, BigDecimal.ZERO,
            List.of(new OrderService.CreateOrderItem(1L, BigDecimal.ONE, 1, false))));

        assertThat(saved.getPickupAt()).isEqualTo(pickup);
        assertThat(saved.getDeliveryAt()).isEqualTo(delivery);
        }

        @Test
        void rejectsInactiveAndUnpricedServices() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        when(customers.findById(1L)).thenReturn(Optional.of(new Customer("Test", "9876543210", null, null)));
        LaundryServiceItem inactive = mock(LaundryServiceItem.class);
        when(inactive.isActive()).thenReturn(false);
        when(services.findById(1L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> orderService.create(command(1, 1, 1, BigDecimal.ZERO, BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);

        LaundryServiceItem unpriced = new LaundryServiceItem("UNPRICED", "Unpriced", "Ironing", PricingUnit.PIECE, BigDecimal.ZERO);
        when(services.findById(1L)).thenReturn(Optional.of(unpriced));
        assertThatThrownBy(() -> orderService.create(command(1, 1, 1, BigDecimal.ZERO, BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsConflictingReuseOfRequestId() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
        OrderService orderService = new OrderService(orders, customers, services, history, mock(ApplicationEventPublisher.class));
        LaundryOrder existing = new LaundryOrder(
            "same-browser-request", new Customer("Test Customer", "9876543210", null, "Trichy"),
            null, "original", "admin");
        when(orders.findByClientRequestId("same-browser-request")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.create(new OrderService.CreateOrderCommand(
            "same-browser-request", 99L, null, "changed", "admin",
            BigDecimal.ZERO, BigDecimal.ZERO, List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different order");
        }

        private static OrderService.CreateOrderCommand command(
            long serviceId, int quantity, int pieces, BigDecimal discount, BigDecimal tax) {
        return new OrderService.CreateOrderCommand(
            java.util.UUID.randomUUID().toString(), 1L, null, null, "admin", discount, tax,
            List.of(new OrderService.CreateOrderItem(serviceId, BigDecimal.valueOf(quantity), pieces, false)));
        }
}
