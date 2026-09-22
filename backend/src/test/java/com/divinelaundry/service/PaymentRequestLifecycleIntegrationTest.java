package com.divinelaundry.service;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-request-lifecycle;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.admin.username=admin",
        "app.admin.password=TestPassword123!",
        "app.whatsapp.enabled=false",
        "app.payment.upi-id="
})
@ActiveProfiles("test")
class PaymentRequestLifecycleIntegrationTest {
    @Autowired private PaymentRequestService paymentRequestService;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRequestRepository paymentRequests;
    @Autowired private LaundryOrderRepository orders;
    @Autowired private CustomerRepository customers;
    @Autowired private LaundryServiceRepository laundryServices;
    @Autowired private MockPaymentProviderTestSupport mockPaymentProviderTestSupport;

    @Test
    void primaryLifecycleCompletesInTwoVerifiedSteps() {
        Customer customer = customers.save(new Customer("Test Customer", randomPhone(), null, "Trichy"));
        LaundryOrder order = createOrderWithTotal(customer, "SO-2026-000175", new BigDecimal("1725.00"));

        PaymentRequest firstRequest = paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("1000.00"), "admin", "req-1725-1000");
        assertThat(firstRequest.getRequestedAmount()).isEqualByComparingTo("1000.00");
        assertThat(firstRequest.getProviderReference()).isNotBlank();
        assertThat(firstRequest.getPaymentUrl()).contains("mock-local://payment/");
        assertThat(firstRequest.getQrPayload()).contains("upi://pay?");
        mockPaymentProviderTestSupport.verify(firstRequest.getProviderReference(), new BigDecimal("1000.00"), "INR");

        PaymentRequest firstPaid = paymentRequestService.confirmVerifiedPayment(order.getOrderNumber(), firstRequest.getProviderReference(), new BigDecimal("1000.00"), "INR", "admin");
        assertThat(firstPaid.getStatus()).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(paymentService.summary(order.getOrderNumber()).balance()).isEqualByComparingTo("725.00");

        PaymentRequest secondRequest = paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("725.00"), "admin", "req-1725-725");
        assertThat(secondRequest.getRequestedAmount()).isEqualByComparingTo("725.00");
        mockPaymentProviderTestSupport.verify(secondRequest.getProviderReference(), new BigDecimal("725.00"), "INR");

        PaymentRequest secondPaid = paymentRequestService.confirmVerifiedPayment(order.getOrderNumber(), secondRequest.getProviderReference(), new BigDecimal("725.00"), "INR", "admin");
        assertThat(secondPaid.getStatus()).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(paymentService.summary(order.getOrderNumber()).balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void idempotencyKeyReusedWithDifferentAmountIsRejected() {
        Customer customer = customers.save(new Customer("Test Customer A", randomPhone(), null, "Trichy"));
        LaundryOrder order = createOrderWithTotal(customer, "SO-2026-000176", new BigDecimal("1725.00"));

        paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("1000.00"), "admin", "req-dup-amount");

        assertThatThrownBy(() -> paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("900.00"), "admin", "req-dup-amount"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different order or amount");
    }

    @Test
    void idempotencyKeyReusedForDifferentOrderIsRejected() {
        Customer firstCustomer = customers.save(new Customer("Customer One", randomPhone(), null, "Trichy"));
        LaundryOrder firstOrder = createOrderWithTotal(firstCustomer, "SO-2026-000177", new BigDecimal("1725.00"));
        Customer secondCustomer = customers.save(new Customer("Customer Two", randomPhone(), null, "Trichy"));
        LaundryOrder secondOrder = createOrderWithTotal(secondCustomer, "SO-2026-000178", new BigDecimal("1000.00"));

        paymentRequestService.createPaymentRequest(firstOrder.getOrderNumber(), new BigDecimal("1000.00"), "admin", "req-shared-key");

        assertThatThrownBy(() -> paymentRequestService.createPaymentRequest(secondOrder.getOrderNumber(), new BigDecimal("1000.00"), "admin", "req-shared-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different order or amount");
    }

    @Test
    void requestAboveOutstandingAndRequestWhenAlreadyPaidAreRejected() {
        Customer customer = customers.save(new Customer("Test Customer B", randomPhone(), null, "Trichy"));
        LaundryOrder order = createOrderWithTotal(customer, "SO-2026-000179", new BigDecimal("1725.00"));

        PaymentRequest firstRequest = paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("1000.00"), "admin", "req-1725-1000-2");
        mockPaymentProviderTestSupport.verify(firstRequest.getProviderReference(), new BigDecimal("1000.00"), "INR");
        paymentRequestService.confirmVerifiedPayment(order.getOrderNumber(), firstRequest.getProviderReference(), new BigDecimal("1000.00"), "INR", "admin");

        assertThatThrownBy(() -> paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("800.00"), "admin", "req-1725-800"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outstanding");

        PaymentRequest secondRequest = paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("725.00"), "admin", "req-1725-725-2");
        mockPaymentProviderTestSupport.verify(secondRequest.getProviderReference(), new BigDecimal("725.00"), "INR");
        paymentRequestService.confirmVerifiedPayment(order.getOrderNumber(), secondRequest.getProviderReference(), new BigDecimal("725.00"), "INR", "admin");

        assertThatThrownBy(() -> paymentRequestService.createPaymentRequest(order.getOrderNumber(), new BigDecimal("1.00"), "admin", "req-1725-final-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outstanding");
    }

    private LaundryOrder createOrderWithTotal(Customer customer, String orderNumber, BigDecimal total) {
        LaundryOrder order = new LaundryOrder("client-order-" + UUID.randomUUID(), customer, null, null, "admin");
        LaundryServiceItem service = laundryServices.save(new LaundryServiceItem(orderNumber + "-SERVICE", "Laundry Bundle", "Laundry", PricingUnit.PIECE, total));
        order.addItem(new OrderItem(service, BigDecimal.ONE, 1, false));
        order.calculateTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        order.assignOrderNumber(orderNumber);
        order.finalizeInvoice("INV-" + orderNumber);
        return orders.save(order);
    }

    private static String randomPhone() {
        return "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1000000000L));
    }
}
