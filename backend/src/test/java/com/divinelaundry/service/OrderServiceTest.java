package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.LaundryServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Test
    void repeatedClientRequestReturnsTheOriginalOrderWithoutCreatingAnotherBill() {
        LaundryOrderRepository orders = mock(LaundryOrderRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        LaundryServiceRepository services = mock(LaundryServiceRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        OrderService orderService = new OrderService(orders, customers, services, events);
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
        OrderService orderService = new OrderService(orders, customers, services, events);
        LaundryOrder order = mock(LaundryOrder.class);
        when(order.getInvoiceNumber()).thenReturn(null);
        when(order.getId()).thenReturn(42L);
        when(order.getOrderNumber()).thenReturn("SO-2026-000042");
        when(orders.findByOrderNumber("SO-2026-000042")).thenReturn(Optional.of(order));

        orderService.finalizeInvoice("SO-2026-000042");

        verify(order).finalizeInvoice(anyString());
        verify(events).publishEvent(new InvoiceFinalisedEvent("SO-2026-000042"));
    }
}
