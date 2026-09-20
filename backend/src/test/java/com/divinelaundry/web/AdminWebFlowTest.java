package com.divinelaundry.web;

import com.divinelaundry.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webtests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "app.admin.username=admin",
        "app.admin.password=TestPassword123!", "app.whatsapp.enabled=false", "app.payment.upi-id="})
class AdminWebFlowTest {
    @Autowired WebApplicationContext context;
    @Autowired CustomerRepository customers;
    @Autowired LaundryOrderRepository orders;
    @Autowired LaundryServiceRepository catalog;
        @Autowired PaymentRepository payments;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void loginAndCsrfAreEnforced() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection());
                mvc.perform(get("/api/documents/orders/SO-2026-000001")).andExpect(status().is3xxRedirection());
                        mvc.perform(get("/orders/SO-2026-000001/invoice.pdf")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        var result = mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "TestPassword123!"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
    }

    @Test void whatsappRetryRequiresAdminAndCsrf() throws Exception {
        mvc.perform(post("/orders/unknown/whatsapp/1/retry"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/orders/unknown/whatsapp/1/retry").with(user("staff")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/orders/unknown/whatsapp/1/retry").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test void customerOrderInvoiceAndPaymentFlowUsesDatabase() throws Exception {
        String phone = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        long customersBefore = customers.count();
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Test Customer").param("phone", "+91 " + phone).param("area", "Trichy").param("addressLine", "Test street"))
                .andExpect(status().is3xxRedirection());
        assertThat(customers.count()).isEqualTo(customersBefore + 1);
        var customer = customers.findByPhone(phone).orElseThrow();
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Duplicate").param("phone", phone)).andExpect(view().name("customer-form"))
                .andExpect(model().attributeHasErrors("customerForm"));
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(s -> s.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String requestId = UUID.randomUUID().toString();
        long before = orders.count();
        String redirect = createOrder(customer.getId(), service.getId(), requestId);
        assertThat(createOrder(customer.getId(), service.getId(), requestId)).isEqualTo(redirect);
        assertThat(orders.count()).isEqualTo(before + 1);
        String number = redirect.substring("/orders/".length());
        var order = orders.findByOrderNumber(number).orElseThrow();
        assertThat(order.getTotal()).isEqualByComparingTo("28.00");
        assertThat(order.getInvoiceNumber()).isNotBlank();
        mvc.perform(get(redirect).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get(redirect + "/invoice").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(order.getInvoiceNumber())));
        mvc.perform(get(redirect + "/invoice.png").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        var firstPdf = mvc.perform(get(redirect + "/invoice.pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(order.getInvoiceNumber() + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        var secondPdf = mvc.perform(get(redirect + "/invoice.pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(firstPdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(secondPdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(firstPdf.length).isEqualTo(secondPdf.length);
        mvc.perform(post(redirect + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", UUID.randomUUID().toString()).param("amount", "29.00").param("mode", "CASH"))
                .andExpect(model().attributeHasErrors("paymentForm"));
        mvc.perform(post(redirect + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", UUID.randomUUID().toString()).param("amount", "10.00").param("mode", "CASH"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(get(redirect + "/invoice").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PARTIAL")));
        String paymentId = UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) mvc.perform(post(redirect + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", paymentId).param("amount", "18.00").param("mode", "CASH"))
                .andExpect(redirectedUrl(redirect));
        assertThat(orders.findByOrderNumber(number).orElseThrow().getPaymentStatus().name()).isEqualTo("PAID");
        mvc.perform(post(redirect + "/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "WASHING"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(post(redirect + "/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "IRONING"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(post(redirect + "/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "CLEANED"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(post(redirect + "/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "READY"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(post(redirect + "/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "DELIVERED"))
                .andExpect(redirectedUrl(redirect));
        mvc.perform(get(redirect + "/invoice").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get(redirect + "/invoice.pdf").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        assertThat(createOrder(customer.getId(), service.getId(), UUID.randomUUID().toString())).isNotEqualTo(redirect);
        assertThat(orders.count()).isEqualTo(before + 2);
    }

    @Test void operationsListAndStatusActionsUseServerRulesAndCsrf() throws Exception {
        String phone = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Operations Customer").param("phone", phone).param("area", "Trichy")
                .param("addressLine", "Test street")).andExpect(status().is3xxRedirection());
        var customer = customers.findByPhone(phone).orElseThrow();
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(item -> item.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String redirect = createOrder(customer.getId(), service.getId(), UUID.randomUUID().toString());
        String number = redirect.substring("/orders/".length());

        mvc.perform(get("/orders").param("orderQuery", number).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(number)));
        mvc.perform(post("/orders/{number}/status", number).with(user("admin").roles("ADMIN"))
                .with(csrf()).param("status", "WASHING")).andExpect(redirectedUrl(redirect));
        assertThat(orders.findByOrderNumber(number).orElseThrow().getWorkStatus().name()).isEqualTo("WASHING");
        mvc.perform(post("/orders/{number}/status", number).with(user("admin").roles("ADMIN"))
                .with(csrf()).param("status", "DELIVERED")).andExpect(redirectedUrl(redirect));
        assertThat(orders.findByOrderNumber(number).orElseThrow().getWorkStatus().name()).isEqualTo("WASHING");
        mvc.perform(post("/orders/{number}/status", number).with(user("admin").roles("ADMIN"))
                .param("status", "CLEANED")).andExpect(status().isForbidden());
    }

    @Test void paymentReceiptRoutesAreAdminOnlyOrderScopedAndReadOnly() throws Exception {
        String phone = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        mvc.perform(post("/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "Receipt Customer").param("phone", phone).param("area", "Trichy")
                .param("addressLine", "Receipt street")).andExpect(status().is3xxRedirection());
        var customer = customers.findByPhone(phone).orElseThrow();
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(item -> item.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String orderUrl = createOrder(customer.getId(), service.getId(), UUID.randomUUID().toString());
        String orderNumber = orderUrl.substring("/orders/".length());
        String requestId = UUID.randomUUID().toString();
        mvc.perform(post(orderUrl + "/payments").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", requestId).param("amount", "10.00").param("mode", "CASH")
                .param("reference", "RECEIPT-REF")).andExpect(redirectedUrl(orderUrl));
        var payment = payments.findByClientRequestId(requestId).orElseThrow();
        long paymentCount = payments.count();
        String receiptUrl = "/orders/" + orderNumber + "/payments/" + payment.getPaymentNumber() + "/receipt";
        mvc.perform(get(receiptUrl)).andExpect(status().is3xxRedirection());
        mvc.perform(get(receiptUrl).with(user("cashier").roles("CASHIER"))).andExpect(status().isForbidden());
        mvc.perform(get(receiptUrl).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(payment.getPaymentNumber())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PARTIAL")));
        var firstReceiptPdf = mvc.perform(get(receiptUrl + ".pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(payment.getPaymentNumber())))
                .andReturn().getResponse().getContentAsByteArray();
        var secondReceiptPdf = mvc.perform(get(receiptUrl + ".pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(firstReceiptPdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(secondReceiptPdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(payments.count()).isEqualTo(paymentCount);
        mvc.perform(get("/orders/OTHER-ORDER/payments/" + payment.getPaymentNumber() + "/receipt")
                .with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    }

    @Test void restOrderUsesAuthenticatedActorAndCustomerPhoneNormalization() throws Exception {
        String digits = "9" + String.format("%09d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1000000000L));
        mvc.perform(post("/api/customers").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"name\":\"REST Customer\",\"phone\":\"+91 %s\",\"addressLine\":\"Test\",\"area\":\"Trichy\"}").formatted(digits)))
                .andExpect(status().isCreated());
        assertThat(customers.findByPhone(digits)).isPresent();

        var customer = customers.findByPhone(digits).orElseThrow();
        var service = catalog.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .filter(item -> item.getCode().equals("SHIRT_IRON")).findFirst().orElseThrow();
        String requestId = UUID.randomUUID().toString();
        String body = ("{\"clientRequestId\":\"%s\",\"customerId\":%d,\"deliveryAt\":\"2027-01-15T18:00:00Z\",\"notes\":\"REST\",\"createdBy\":\"spoofed\",\"discount\":0,\"tax\":0,\"items\":[{\"serviceId\":%d,\"billableQuantity\":2,\"pieceCount\":2,\"noPrint\":false}]}")
                .formatted(requestId, customer.getId(), service.getId());
        var result = mvc.perform(post("/api/orders").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String orderNumber = result.getResponse().getContentAsString()
                .replaceAll(".*\\\"orderNumber\\\":\\\"([^\\\"]+)\\\".*", "$1");
        assertThat(orders.findByOrderNumber(orderNumber).orElseThrow().getCreatedBy()).isEqualTo("admin");
    }

    private String createOrder(Long customerId, Long serviceId, String requestId) throws Exception {
        return mvc.perform(post("/orders").with(user("admin").roles("ADMIN")).with(csrf())
                .param("requestId", requestId).param("customerId", customerId.toString()).param("deliveryAt", "2027-01-15T18:00")
                .param("items[0].serviceId", serviceId.toString()).param("items[0].quantity", "2").param("items[0].pieces", "2")
                .param("discount", "0").param("tax", "0"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }
}
